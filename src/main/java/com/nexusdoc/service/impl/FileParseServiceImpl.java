package com.nexusdoc.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusdoc.common.config.FileMcpProperties;
import com.nexusdoc.common.exception.BusinessException;
import com.nexusdoc.service.FileParseService;
import com.nexusdoc.vo.FileExtractResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileParseServiceImpl implements FileParseService {

    private static final int PREVIEW_LENGTH = 600;

    private final FileMcpProperties properties;

    private final ObjectMapper objectMapper;

    @Override
    public FileExtractResultVO extract(MultipartFile file) {
        validateFile(file);
        String fileName = sanitizeFileName(file.getOriginalFilename());
        String fileType = resolveFileType(fileName);
        byte[] bytes = readBytes(file);
        List<String> warnings = new ArrayList<>();
        ParseTextResult parseTextResult = switch (fileType) {
            case "txt", "md", "markdown", "csv" -> new ParseTextResult(readPlainText(bytes), null, null);
            case "json" -> new ParseTextResult(readJsonText(bytes), null, null);
            case "docx" -> new ParseTextResult(readDocx(bytes), null, null);
            case "pdf" -> readPdf(bytes, warnings);
            default -> throw unsupportedTypeException();
        };

        String normalizedText = normalizeText(parseTextResult.text());
        if (!StringUtils.hasText(normalizedText)) {
            throw new BusinessException("未能从文件中提取到有效文本，请检查文件内容是否为空或为扫描图片。");
        }

        int originalLength = normalizedText.length();
        int maxExtractChars = Math.max(properties.getMaxExtractChars(), 1);
        boolean truncated = originalLength > maxExtractChars;
        String usedText = truncated ? normalizedText.substring(0, maxExtractChars) : normalizedText;
        if (truncated) {
            warnings.add("文件正文超过本次提取上限，已截取前 %d 字用于 AI 生成。".formatted(maxExtractChars));
        }

        log.info("FileInsight MCP 文件解析完成，fileName={}, fileType={}, fileSize={}, originalLength={}, usedLength={}, truncated={}",
                fileName, fileType, file.getSize(), originalLength, usedText.length(), truncated);
        return FileExtractResultVO.builder()
                .fileName(fileName)
                .fileType(fileType)
                .fileSize(file.getSize())
                .textPreview(buildPreview(usedText))
                .originalTextLength(originalLength)
                .usedTextLength(usedText.length())
                .truncated(truncated)
                .pageCount(parseTextResult.pageCount())
                .parsedPageCount(parseTextResult.parsedPageCount())
                .parserName(parserName(fileType))
                .warnings(warnings)
                .extractedText(usedText)
                .build();
    }

    private void validateFile(MultipartFile file) {
        if (Boolean.FALSE.equals(properties.getEnabled())) {
            throw new BusinessException("FileInsight MCP 当前未启用。");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空。");
        }
        long maxBytes = Math.max(properties.getMaxFileSizeMb(), 1) * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessException("文件过大，当前最大支持 %dMB，请压缩或拆分后再上传。"
                    .formatted(properties.getMaxFileSizeMb()));
        }
        String fileType = resolveFileType(sanitizeFileName(file.getOriginalFilename()));
        boolean supported = properties.getSupportedTypes().stream()
                .anyMatch(type -> type.equalsIgnoreCase(fileType));
        if (!supported) {
            throw unsupportedTypeException();
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException("文件读取失败，请更换文件或复制文本后重试。");
        }
    }

    private String readPlainText(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
            return new String(bytes, StandardCharsets.UTF_16BE);
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
            return new String(bytes, StandardCharsets.UTF_16LE);
        }
        String utf8 = new String(bytes, StandardCharsets.UTF_8);
        int utf8ReplacementCount = countReplacementChars(utf8);
        if (utf8ReplacementCount == 0) {
            return utf8;
        }
        String gb18030 = new String(bytes, Charset.forName("GB18030"));
        return countReplacementChars(gb18030) < utf8ReplacementCount ? gb18030 : utf8;
    }

    private String readJsonText(byte[] bytes) {
        String text = readPlainText(bytes);
        try {
            Object json = objectMapper.readValue(text, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception ignored) {
            return text;
        }
    }

    private String readDocx(byte[] bytes) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            StringBuilder builder = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                appendLine(builder, paragraph.getText());
            }
            for (XWPFTable table : document.getTables()) {
                appendTable(builder, table);
            }
            return builder.toString();
        } catch (Exception exception) {
            log.warn("DOCX 文件解析失败：{}", exception.getMessage());
            throw new BusinessException("文件解析失败，请更换文件或复制文本后重试。");
        }
    }

    private ParseTextResult readPdf(byte[] bytes, List<String> warnings) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                throw new BusinessException("当前 PDF 已加密，暂时无法解析，请先导出无加密版本或复制文本后重试。");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            int pageCount = document.getNumberOfPages();
            int maxPages = Math.max(properties.getMaxPdfPages(), 1);
            int parsedPageCount = Math.min(pageCount, maxPages);
            int maxChars = Math.max(properties.getMaxExtractChars(), 1);
            StringBuilder builder = new StringBuilder();
            for (int page = 1; page <= parsedPageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                builder.append(stripper.getText(document)).append('\n');
                if (builder.length() >= maxChars) {
                    warnings.add("PDF 文本较长，已达到本次提取字符上限，后续内容未进入 AI 生成。");
                    break;
                }
            }
            if (pageCount > maxPages) {
                warnings.add("PDF 页数较多，仅解析前 %d 页。".formatted(maxPages));
            }
            String text = builder.toString();
            if (!StringUtils.hasText(text)) {
                warnings.add("未检测到可复制文本；如果这是扫描件或图片版 PDF，需要先 OCR 后再上传。");
            }
            return new ParseTextResult(text, pageCount, parsedPageCount);
        } catch (Exception exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            log.warn("PDF 文件解析失败：{}", exception.getMessage());
            throw new BusinessException("文件解析失败，请更换文件或复制文本后重试。");
        }
    }

    private void appendTable(StringBuilder builder, XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            List<String> cellTexts = new ArrayList<>();
            for (XWPFTableCell cell : row.getTableCells()) {
                String text = normalizeCellText(cell.getText());
                if (StringUtils.hasText(text)) {
                    cellTexts.add(text);
                }
                for (XWPFTable nestedTable : cell.getTables()) {
                    appendTable(builder, nestedTable);
                }
            }
            if (!cellTexts.isEmpty()) {
                appendLine(builder, String.join(" | ", cellTexts));
            }
        }
    }

    private String resolveFileType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw unsupportedTypeException();
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "untitled";
        }
        return fileName.replace("\\", "/").substring(fileName.replace("\\", "/").lastIndexOf('/') + 1);
    }

    private String normalizeText(String text) {
        return StringUtils.hasText(text)
                ? text.replace("\u0000", "")
                .replace('\u00A0', ' ')
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim()
                : "";
    }

    private String normalizeCellText(String text) {
        return StringUtils.hasText(text)
                ? text.replaceAll("\\s+", " ").trim()
                : "";
    }

    private String buildPreview(String text) {
        String preview = text.replaceAll("\\s+", " ").trim();
        return preview.length() > PREVIEW_LENGTH ? preview.substring(0, PREVIEW_LENGTH) + "..." : preview;
    }

    private String parserName(String fileType) {
        return switch (fileType) {
            case "docx" -> "Apache POI XWPF";
            case "pdf" -> "Apache PDFBox";
            case "json" -> "Jackson JSON Text Parser";
            default -> "NexusDoc Text Parser";
        };
    }

    private void appendLine(StringBuilder builder, String text) {
        if (StringUtils.hasText(text)) {
            builder.append(text.trim()).append('\n');
        }
    }

    private int countReplacementChars(String text) {
        int count = 0;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == '\uFFFD') {
                count++;
            }
        }
        return count;
    }

    private record ParseTextResult(String text, Integer pageCount, Integer parsedPageCount) {
    }

    private BusinessException unsupportedTypeException() {
        return new BusinessException("当前文件类型暂不支持，请上传 txt、md、csv、json、docx 或 pdf 文件。");
    }
}
