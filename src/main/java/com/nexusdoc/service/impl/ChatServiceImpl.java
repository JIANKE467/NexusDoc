package com.nexusdoc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusdoc.ai.PromptTemplateFactory;
import com.nexusdoc.common.exception.BusinessException;
import com.nexusdoc.common.exception.DatabaseExceptionHelper;
import com.nexusdoc.dto.ChatAskRequest;
import com.nexusdoc.entity.ChatRecord;
import com.nexusdoc.entity.Document;
import com.nexusdoc.entity.DocumentPackage;
import com.nexusdoc.mapper.ChatRecordMapper;
import com.nexusdoc.mapper.DocumentMapper;
import com.nexusdoc.mapper.DocumentPackageMapper;
import com.nexusdoc.service.AiService;
import com.nexusdoc.service.ChatService;
import com.nexusdoc.service.support.InMemoryDocumentStore;
import com.nexusdoc.vo.ChatAnswerVO;
import com.nexusdoc.vo.ChatRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final Long ANONYMOUS_USER_ID = 0L;

    private final DocumentMapper documentMapper;
    private final DocumentPackageMapper documentPackageMapper;
    private final ChatRecordMapper chatRecordMapper;
    private final AiService aiService;
    private final InMemoryDocumentStore inMemoryDocumentStore;

    @Override
    public ChatAnswerVO ask(ChatAskRequest request) {
        validateAskRequest(request);
        Long userId = resolveUserId(request.getUserId());

        Document document;
        boolean inMemoryMode = false;
        try {
            document = documentMapper.selectById(request.getDocumentId());
        } catch (RuntimeException exception) {
            if (!DatabaseExceptionHelper.isDatabaseUnavailable(exception)) {
                throw exception;
            }
            inMemoryMode = true;
            document = inMemoryDocumentStore.findDocument(request.getDocumentId());
            log.warn("数据库未连接，文档追问使用内存临时存储：{}", exception.getMessage());
        }
        if (document == null || !document.getUserId().equals(userId)) {
            throw new BusinessException("文档不存在或无权访问");
        }

        DocumentPackage documentPackage;
        if (inMemoryMode) {
            documentPackage = inMemoryDocumentStore.findPackage(document.getId());
        } else {
            try {
                documentPackage = documentPackageMapper.selectOne(
                        new LambdaQueryWrapper<DocumentPackage>()
                                .eq(DocumentPackage::getDocumentId, document.getId())
                                .last("LIMIT 1")
                );
            } catch (RuntimeException exception) {
                throw databaseBusinessException(exception);
            }
        }

        String prompt = PromptTemplateFactory.buildAskPrompt(
                document.getContent(),
                documentPackage == null ? "" : documentPackage.getResultText(),
                request.getQuestion().trim()
        );
        String answer = aiService.generate(prompt);

        ChatRecord record = new ChatRecord();
        record.setUserId(userId);
        record.setDocumentId(request.getDocumentId());
        record.setUserQuestion(request.getQuestion().trim());
        record.setAiAnswer(answer);
        record.setCreateTime(LocalDateTime.now());
        if (inMemoryMode) {
            inMemoryDocumentStore.saveChatRecord(record);
        } else {
            try {
                chatRecordMapper.insert(record);
            } catch (RuntimeException exception) {
                throw databaseBusinessException(exception);
            }
        }

        log.info("文档追问成功，userId={}, documentId={}, chatRecordId={}",
                userId, request.getDocumentId(), record.getId());
        return ChatAnswerVO.builder()
                .chatRecordId(record.getId())
                .documentId(record.getDocumentId())
                .question(record.getUserQuestion())
                .answer(record.getAiAnswer())
                .createTime(record.getCreateTime())
                .build();
    }

    @Override
    public List<ChatRecordVO> listRecords(Long documentId) {
        if (documentId == null) {
            throw new BusinessException("documentId 不能为空");
        }
        try {
            if (documentMapper.selectById(documentId) == null) {
                throw new BusinessException("文档不存在");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            if (!DatabaseExceptionHelper.isDatabaseUnavailable(exception)) {
                throw exception;
            }
            if (inMemoryDocumentStore.findDocument(documentId) == null) {
                throw new BusinessException("文档不存在");
            }
            log.warn("数据库未连接，追问记录使用内存临时存储：{}", exception.getMessage());
            return inMemoryDocumentStore.listChatRecords(documentId).stream()
                    .map(record -> ChatRecordVO.builder()
                            .id(record.getId())
                            .question(record.getUserQuestion())
                            .answer(record.getAiAnswer())
                            .createTime(record.getCreateTime())
                            .build())
                    .toList();
        }
        try {
            return chatRecordMapper.selectList(new LambdaQueryWrapper<ChatRecord>()
                            .eq(ChatRecord::getDocumentId, documentId)
                            .orderByAsc(ChatRecord::getCreateTime))
                    .stream()
                    .map(record -> ChatRecordVO.builder()
                            .id(record.getId())
                            .question(record.getUserQuestion())
                            .answer(record.getAiAnswer())
                            .createTime(record.getCreateTime())
                            .build())
                    .toList();
        } catch (RuntimeException exception) {
            throw databaseBusinessException(exception);
        }
    }

    private BusinessException databaseBusinessException(RuntimeException exception) {
        if (DatabaseExceptionHelper.isDatabaseUnavailable(exception)) {
            return new BusinessException(DatabaseExceptionHelper.DATABASE_UNAVAILABLE_MESSAGE);
        }
        throw exception;
    }

    private void validateAskRequest(ChatAskRequest request) {
        if (request == null
                || request.getDocumentId() == null
                || !StringUtils.hasText(request.getQuestion())) {
            throw new BusinessException("documentId 和问题不能为空");
        }
    }

    private Long resolveUserId(Long userId) {
        return userId == null ? ANONYMOUS_USER_ID : userId;
    }
}
