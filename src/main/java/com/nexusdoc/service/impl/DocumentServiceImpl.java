package com.nexusdoc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusdoc.ai.PromptTemplateFactory;
import com.nexusdoc.common.exception.BusinessException;
import com.nexusdoc.dto.DocumentGenerateRequest;
import com.nexusdoc.entity.ChatRecord;
import com.nexusdoc.entity.Document;
import com.nexusdoc.entity.DocumentPackage;
import com.nexusdoc.entity.User;
import com.nexusdoc.enums.DocumentTypeEnum;
import com.nexusdoc.mapper.ChatRecordMapper;
import com.nexusdoc.mapper.DocumentMapper;
import com.nexusdoc.mapper.DocumentPackageMapper;
import com.nexusdoc.mapper.UserMapper;
import com.nexusdoc.service.AiService;
import com.nexusdoc.service.DocumentService;
import com.nexusdoc.vo.DocumentDetailVO;
import com.nexusdoc.vo.DocumentListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final int MAX_CONTENT_LENGTH = 20000;

    private final UserMapper userMapper;
    private final DocumentMapper documentMapper;
    private final DocumentPackageMapper documentPackageMapper;
    private final ChatRecordMapper chatRecordMapper;
    private final AiService aiService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentDetailVO generateDocument(DocumentGenerateRequest request) {
        validateGenerateRequest(request);
        ensureUserExists(request.getUserId());

        Document document = new Document();
        document.setUserId(request.getUserId());
        document.setTitle(request.getTitle().trim());
        document.setDocType(normalizeDocType(request.getDocType()));
        document.setTag(StringUtils.hasText(request.getTag()) ? request.getTag().trim() : null);
        document.setContent(request.getContent().trim());
        document.setCreateTime(LocalDateTime.now());
        documentMapper.insert(document);

        String prompt = PromptTemplateFactory.buildDocumentPrompt(document.getDocType(), document.getContent());
        String resultText = aiService.chat(prompt);

        DocumentPackage documentPackage = new DocumentPackage();
        documentPackage.setDocumentId(document.getId());
        documentPackage.setResultText(resultText);
        documentPackage.setCreateTime(LocalDateTime.now());
        documentPackageMapper.insert(documentPackage);

        log.info("文档生成成功，userId={}, documentId={}", request.getUserId(), document.getId());
        return buildDetailVO(document, documentPackage);
    }

    @Override
    public List<DocumentListVO> listDocuments(Long userId) {
        if (userId == null) {
            throw new BusinessException("userId 不能为空");
        }
        ensureUserExists(userId);

        return documentMapper.selectList(new LambdaQueryWrapper<Document>()
                        .eq(Document::getUserId, userId)
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
    }

    @Override
    public DocumentDetailVO getDocumentDetail(Long documentId) {
        Document document = getDocumentOrThrow(documentId);
        return buildDetailVO(document, findPackageByDocumentId(documentId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId) {
        getDocumentOrThrow(documentId);
        documentPackageMapper.delete(new LambdaQueryWrapper<DocumentPackage>()
                .eq(DocumentPackage::getDocumentId, documentId));
        chatRecordMapper.delete(new LambdaQueryWrapper<ChatRecord>()
                .eq(ChatRecord::getDocumentId, documentId));
        documentMapper.deleteById(documentId);
        log.info("文档删除成功，documentId={}", documentId);
    }

    private void validateGenerateRequest(DocumentGenerateRequest request) {
        if (request == null
                || request.getUserId() == null
                || !StringUtils.hasText(request.getTitle())
                || !StringUtils.hasText(request.getDocType())
                || !StringUtils.hasText(request.getContent())) {
            throw new BusinessException("文档标题、类型、正文和 userId 不能为空");
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
                : DocumentTypeEnum.GENERAL_SUMMARY.getDisplayName();
    }

    private void ensureUserExists(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
    }

    private Document getDocumentOrThrow(Long documentId) {
        if (documentId == null) {
            throw new BusinessException("documentId 不能为空");
        }
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        return document;
    }

    private DocumentPackage findPackageByDocumentId(Long documentId) {
        return documentPackageMapper.selectOne(new LambdaQueryWrapper<DocumentPackage>()
                .eq(DocumentPackage::getDocumentId, documentId)
                .last("LIMIT 1"));
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
}
