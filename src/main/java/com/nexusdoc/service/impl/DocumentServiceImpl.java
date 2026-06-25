package com.nexusdoc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusdoc.ai.PromptTemplateFactory;
import com.nexusdoc.common.exception.BusinessException;
import com.nexusdoc.common.exception.DatabaseExceptionHelper;
import com.nexusdoc.dto.DocumentGenerateRequest;
import com.nexusdoc.entity.ChatRecord;
import com.nexusdoc.entity.Document;
import com.nexusdoc.entity.DocumentPackage;
import com.nexusdoc.enums.DocumentTypeEnum;
import com.nexusdoc.mapper.ChatRecordMapper;
import com.nexusdoc.mapper.DocumentMapper;
import com.nexusdoc.mapper.DocumentPackageMapper;
import com.nexusdoc.service.AiService;
import com.nexusdoc.service.DocumentService;
import com.nexusdoc.service.WebSearchService;
import com.nexusdoc.service.support.InMemoryDocumentStore;
import com.nexusdoc.vo.DocumentDetailVO;
import com.nexusdoc.vo.DocumentListVO;
import com.nexusdoc.vo.WebSearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final int MAX_CONTENT_LENGTH = 20000;
    private static final Long ANONYMOUS_USER_ID = 0L;

    private final DocumentMapper documentMapper;
    private final DocumentPackageMapper documentPackageMapper;
    private final ChatRecordMapper chatRecordMapper;
    private final AiService aiService;
    private final WebSearchService webSearchService;
    private final InMemoryDocumentStore inMemoryDocumentStore;

    @Override
    public DocumentDetailVO generateDocument(DocumentGenerateRequest request) {
        validateGenerateRequest(request);
        Long userId = resolveUserId(request.getUserId());

        Document document = new Document();
        document.setUserId(userId);
        document.setTitle(request.getTitle().trim());
        document.setDocType(normalizeDocType(request.getDocType()));
        document.setTag(StringUtils.hasText(request.getTag()) ? request.getTag().trim() : null);
        document.setContent(request.getContent().trim());
        document.setCreateTime(LocalDateTime.now());
        boolean inMemoryMode = false;
        try {
            documentMapper.insert(document);
        } catch (RuntimeException exception) {
            if (!DatabaseExceptionHelper.isDatabaseUnavailable(exception)) {
                throw exception;
            }
            inMemoryMode = true;
            inMemoryDocumentStore.saveDocument(document);
            log.warn("数据库未连接，文档生成切换到内存临时存储：{}", exception.getMessage());
        }

        String resultText;
        try {
            boolean enableWebSearch = Boolean.TRUE.equals(request.getEnableWebSearch());
            List<WebSearchResultVO> searchResults = enableWebSearch
                    ? webSearchService.search(buildSearchQuery(document.getDocType(), document.getContent()))
                    : List.of();
            String prompt = PromptTemplateFactory.buildDocumentPrompt(
                    document.getDocType(),
                    document.getContent(),
                    enableWebSearch,
                    searchResults);
            resultText = aiService.generate(prompt);
        } catch (RuntimeException exception) {
            if (inMemoryMode) {
                inMemoryDocumentStore.deleteDocument(document.getId());
            } else {
                rollbackDocument(document.getId());
            }
            throw exception;
        }

        DocumentPackage documentPackage = new DocumentPackage();
        documentPackage.setDocumentId(document.getId());
        documentPackage.setResultText(resultText);
        documentPackage.setCreateTime(LocalDateTime.now());
        if (inMemoryMode) {
            inMemoryDocumentStore.savePackage(documentPackage);
        } else {
            try {
                documentPackageMapper.insert(documentPackage);
            } catch (RuntimeException exception) {
                rollbackDocument(document.getId());
                throw databaseBusinessException(exception);
            }
        }

        log.info("文档生成成功，userId={}, documentId={}", userId, document.getId());
        return buildDetailVO(document, documentPackage);
    }

    @Override
    public List<DocumentListVO> listDocuments(Long userId) {
        Long resolvedUserId = resolveUserId(userId);

        try {
            return documentMapper.selectList(new LambdaQueryWrapper<Document>()
                            .eq(Document::getUserId, resolvedUserId)
                            .orderByDesc(Document::getCreateTime))
                    .stream()
                    .map(document -> {
                        DocumentPackage documentPackage = findPackageByDocumentId(document.getId());
                        return DocumentListVO.builder()
                                .documentId(document.getId())
                                .title(document.getTitle())
                                .docType(document.getDocType())
                                .tag(document.getTag())
                                .summaryPreview(buildPreview(documentPackage))
                                .createTime(document.getCreateTime())
                                .build();
                    })
                    .toList();
        } catch (RuntimeException exception) {
            if (DatabaseExceptionHelper.isDatabaseUnavailable(exception)) {
                log.warn("数据库未连接，历史记录使用内存临时存储：{}", exception.getMessage());
                return inMemoryDocumentStore.listDocuments(resolvedUserId).stream()
                        .map(document -> {
                            DocumentPackage documentPackage = inMemoryDocumentStore.findPackage(document.getId());
                            return DocumentListVO.builder()
                                    .documentId(document.getId())
                                    .title(document.getTitle())
                                    .docType(document.getDocType())
                                    .tag(document.getTag())
                                    .summaryPreview(buildPreview(documentPackage))
                                    .createTime(document.getCreateTime())
                                    .build();
                        })
                        .toList();
            }
            throw exception;
        }
    }

    @Override
    public DocumentDetailVO getDocumentDetail(Long documentId) {
        Document document = getDocumentOrThrow(documentId);
        return buildDetailVO(document, findPackageByDocumentId(documentId));
    }

    @Override
    public void deleteDocument(Long documentId) {
        Document document = getDocumentOrThrow(documentId);
        if (inMemoryDocumentStore.findDocument(document.getId()) != null) {
            inMemoryDocumentStore.deleteDocument(documentId);
            log.info("内存文档删除成功，documentId={}", documentId);
            return;
        }
        try {
            documentPackageMapper.delete(new LambdaQueryWrapper<DocumentPackage>()
                    .eq(DocumentPackage::getDocumentId, documentId));
            chatRecordMapper.delete(new LambdaQueryWrapper<ChatRecord>()
                    .eq(ChatRecord::getDocumentId, documentId));
            documentMapper.deleteById(documentId);
        } catch (RuntimeException exception) {
            throw databaseBusinessException(exception);
        }
        log.info("文档删除成功，documentId={}", documentId);
    }

    private void validateGenerateRequest(DocumentGenerateRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getTitle())
                || !StringUtils.hasText(request.getDocType())
                || !StringUtils.hasText(request.getContent())) {
            throw new BusinessException("文档标题、类型和正文不能为空");
        }
        if (request.getTitle().trim().length() > 100) {
            throw new BusinessException("文档标题不能超过 100 个字符");
        }
        if (request.getContent().trim().length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException("文档正文不能超过 20000 个字符");
        }
    }

    private String normalizeDocType(String docType) {
        return DocumentTypeEnum.supports(docType.trim())
                ? docType.trim()
                : DocumentTypeEnum.FREE_CHAT.getDisplayName();
    }

    private Long resolveUserId(Long userId) {
        return userId == null ? ANONYMOUS_USER_ID : userId;
    }

    private Document getDocumentOrThrow(Long documentId) {
        if (documentId == null) {
            throw new BusinessException("documentId 不能为空");
        }
        Document document;
        try {
            document = documentMapper.selectById(documentId);
        } catch (RuntimeException exception) {
            if (!DatabaseExceptionHelper.isDatabaseUnavailable(exception)) {
                throw exception;
            }
            log.warn("数据库未连接，文档详情使用内存临时存储：{}", exception.getMessage());
            document = inMemoryDocumentStore.findDocument(documentId);
        }
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        return document;
    }

    private DocumentPackage findPackageByDocumentId(Long documentId) {
        try {
            return documentPackageMapper.selectOne(new LambdaQueryWrapper<DocumentPackage>()
                    .eq(DocumentPackage::getDocumentId, documentId)
                    .last("LIMIT 1"));
        } catch (RuntimeException exception) {
            if (DatabaseExceptionHelper.isDatabaseUnavailable(exception)) {
                return inMemoryDocumentStore.findPackage(documentId);
            }
            throw databaseBusinessException(exception);
        }
    }

    private DocumentDetailVO buildDetailVO(Document document, DocumentPackage documentPackage) {
        return DocumentDetailVO.builder()
                .documentId(document.getId())
                .userId(document.getUserId())
                .title(document.getTitle())
                .docType(document.getDocType())
                .tag(document.getTag())
                .content(document.getContent())
                .resultText(documentPackage == null ? "" : documentPackage.getResultText())
                .createTime(document.getCreateTime())
                .build();
    }

    private String buildPreview(DocumentPackage documentPackage) {
        if (documentPackage == null || !StringUtils.hasText(documentPackage.getResultText())) {
            return "";
        }
        String resultText = documentPackage.getResultText().replaceAll("\\s+", " ").trim();
        return resultText.length() > 120 ? resultText.substring(0, 120) + "..." : resultText;
    }

    private BusinessException databaseBusinessException(RuntimeException exception) {
        if (DatabaseExceptionHelper.isDatabaseUnavailable(exception)) {
            return new BusinessException(DatabaseExceptionHelper.DATABASE_UNAVAILABLE_MESSAGE);
        }
        throw exception;
    }

    private String buildSearchQuery(String docType, String content) {
        String compactContent = content.replaceAll("\\s+", " ").trim();
        String keywordSource = compactContent.length() > 120
                ? compactContent.substring(0, 120)
                : compactContent;
        return docType + " " + keywordSource;
    }

    private void rollbackDocument(Long documentId) {
        try {
            documentMapper.deleteById(documentId);
        } catch (RuntimeException exception) {
            log.warn("文档生成失败后清理数据库记录失败，documentId={}", documentId, exception);
        }
    }
}
