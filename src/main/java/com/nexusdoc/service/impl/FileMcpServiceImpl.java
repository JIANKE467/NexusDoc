package com.nexusdoc.service.impl;

import com.nexusdoc.common.config.FileMcpProperties;
import com.nexusdoc.dto.DocumentGenerateRequest;
import com.nexusdoc.service.DocumentService;
import com.nexusdoc.service.FileMcpService;
import com.nexusdoc.service.FileParseService;
import com.nexusdoc.vo.DocumentDetailVO;
import com.nexusdoc.vo.FileExtractResultVO;
import com.nexusdoc.vo.FileMcpCapabilitiesVO;
import com.nexusdoc.vo.FileMcpGenerateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileMcpServiceImpl implements FileMcpService {

    private static final Long ANONYMOUS_USER_ID = 0L;

    private static final int DOCUMENT_CONTENT_LIMIT = 20000;

    private final FileMcpProperties properties;

    private final FileParseService fileParseService;

    private final DocumentService documentService;

    @Override
    public FileMcpCapabilitiesVO getCapabilities() {
        return FileMcpCapabilitiesVO.builder()
                .enabled(properties.getEnabled())
                .name("FileInsight MCP")
                .description("文件内容解析与知识卡片生成能力，支持将本地文件解析后交给 NexusDoc AI 文档流程。")
                .maxFileSizeMb(properties.getMaxFileSizeMb())
                .maxExtractChars(properties.getMaxExtractChars())
                .maxPdfPages(properties.getMaxPdfPages())
                .supportedTypes(properties.getSupportedTypes())
                .build();
    }

    @Override
    public FileExtractResultVO parseFile(MultipartFile file) {
        return withoutFullText(fileParseService.extract(file));
    }

    @Override
    public FileMcpGenerateVO generateFromFile(MultipartFile file,
                                              Long userId,
                                              String mode,
                                              Boolean enableWebSearch,
                                              String cardTypes,
                                              String requirement) {
        FileExtractResultVO extractResult = fileParseService.extract(file);
        PreparedFileContent preparedContent = buildDocumentContent(extractResult, cardTypes, requirement);
        DocumentGenerateRequest request = new DocumentGenerateRequest();
        request.setUserId(userId == null ? ANONYMOUS_USER_ID : userId);
        request.setTitle(buildTitle(extractResult.getFileName()));
        request.setDocType(resolveDocType(mode));
        request.setTag("FileInsight MCP");
        request.setContent(preparedContent.content());
        request.setEnableWebSearch(enableWebSearch);

        DocumentDetailVO document = documentService.generateDocument(request);
        log.info("FileInsight MCP 生成完成，fileName={}, fileType={}, documentId={}, truncated={}",
                extractResult.getFileName(), extractResult.getFileType(), document.getDocumentId(), extractResult.getTruncated());
        return FileMcpGenerateVO.builder()
                .fileName(extractResult.getFileName())
                .fileType(extractResult.getFileType())
                .fileSize(extractResult.getFileSize())
                .originalTextLength(extractResult.getOriginalTextLength())
                .usedTextLength(preparedContent.usedTextLength())
                .truncated(preparedContent.truncated())
                .pageCount(extractResult.getPageCount())
                .parsedPageCount(extractResult.getParsedPageCount())
                .warnings(preparedContent.warnings())
                .documentId(document.getDocumentId())
                .title(document.getTitle())
                .content(document.getContent())
                .resultText(document.getResultText())
                .document(document)
                .build();
    }

    private FileExtractResultVO withoutFullText(FileExtractResultVO result) {
        result.setExtractedText(null);
        return result;
    }

    private String buildTitle(String fileName) {
        String title = fileName;
        int dotIndex = title.lastIndexOf('.');
        if (dotIndex > 0) {
            title = title.substring(0, dotIndex);
        }
        title = StringUtils.hasText(title) ? title.trim() : "FileInsight MCP 文档";
        return title.length() > 100 ? title.substring(0, 100) : title;
    }

    private String resolveDocType(String mode) {
        if (!StringUtils.hasText(mode)) {
            return "智能回答";
        }
        return switch (mode.trim()) {
            case "summary", "通用总结", "文档解析" -> "通用总结";
            case "mindmap", "思维导图" -> "思维导图";
            case "trend", "趋势分析", "趋势与隐藏问题分析" -> "趋势分析";
            case "action", "任务卡", "会议纪要" -> "会议纪要";
            case "novel", "小说设定" -> "小说设定";
            default -> mode.trim();
        };
    }

    private PreparedFileContent buildDocumentContent(FileExtractResultVO extractResult, String cardTypes, String requirement) {
        StringBuilder builder = new StringBuilder();
        builder.append("以下内容来自 FileInsight MCP 文件解析。\n");
        builder.append("文件名：").append(extractResult.getFileName()).append('\n');
        builder.append("文件类型：").append(extractResult.getFileType()).append('\n');
        builder.append("原始文本长度：").append(extractResult.getOriginalTextLength()).append(" 字\n");
        builder.append("本次使用长度：").append(extractResult.getUsedTextLength()).append(" 字\n");
        if (Boolean.TRUE.equals(extractResult.getTruncated())) {
            builder.append("说明：文件内容较长，系统已截取前 ")
                    .append(extractResult.getUsedTextLength())
                    .append(" 字用于本次生成。\n");
        }
        if (extractResult.getPageCount() != null) {
            builder.append("PDF 页数：").append(extractResult.getPageCount()).append('\n');
            builder.append("已解析页数：").append(extractResult.getParsedPageCount()).append('\n');
        }
        if (StringUtils.hasText(cardTypes)) {
            builder.append("期望卡片类型：").append(cardTypes.trim()).append('\n');
        }
        if (StringUtils.hasText(requirement)) {
            builder.append("用户补充要求：").append(requirement.trim()).append('\n');
        }
        builder.append("\n文件正文：\n");
        int textBudget = Math.max(DOCUMENT_CONTENT_LIMIT - builder.length(), 1);
        String extractedText = extractResult.getExtractedText();
        String usedText = extractedText.length() > textBudget
                ? extractedText.substring(0, textBudget)
                : extractedText;
        builder.append(usedText);
        boolean truncated = Boolean.TRUE.equals(extractResult.getTruncated())
                || usedText.length() < extractedText.length();
        java.util.List<String> warnings = new java.util.ArrayList<>(
                extractResult.getWarnings() == null ? java.util.List.of() : extractResult.getWarnings()
        );
        if (usedText.length() < extractedText.length()) {
            warnings.add("为兼容文档生成接口长度限制，FileInsight MCP 已进一步压缩本次进入 AI 的文本。");
        }
        return new PreparedFileContent(builder.toString(), usedText.length(), truncated, warnings);
    }

    private record PreparedFileContent(String content, int usedTextLength, boolean truncated, java.util.List<String> warnings) {
    }
}
