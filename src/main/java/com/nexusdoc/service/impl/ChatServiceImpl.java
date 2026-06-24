package com.nexusdoc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusdoc.ai.PromptTemplateFactory;
import com.nexusdoc.common.exception.BusinessException;
import com.nexusdoc.dto.ChatAskRequest;
import com.nexusdoc.entity.ChatRecord;
import com.nexusdoc.entity.Document;
import com.nexusdoc.entity.DocumentPackage;
import com.nexusdoc.entity.User;
import com.nexusdoc.mapper.ChatRecordMapper;
import com.nexusdoc.mapper.DocumentMapper;
import com.nexusdoc.mapper.DocumentPackageMapper;
import com.nexusdoc.mapper.UserMapper;
import com.nexusdoc.service.AiService;
import com.nexusdoc.service.ChatService;
import com.nexusdoc.vo.ChatAnswerVO;
import com.nexusdoc.vo.ChatRecordVO;
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
public class ChatServiceImpl implements ChatService {

    private final UserMapper userMapper;
    private final DocumentMapper documentMapper;
    private final DocumentPackageMapper documentPackageMapper;
    private final ChatRecordMapper chatRecordMapper;
    private final AiService aiService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatAnswerVO ask(ChatAskRequest request) {
        validateAskRequest(request);
        User user = userMapper.selectById(request.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        Document document = documentMapper.selectById(request.getDocumentId());
        if (document == null || !document.getUserId().equals(request.getUserId())) {
            throw new BusinessException("文档不存在或无权访问");
        }

        DocumentPackage documentPackage = documentPackageMapper.selectOne(
                new LambdaQueryWrapper<DocumentPackage>()
                        .eq(DocumentPackage::getDocumentId, document.getId())
                        .last("LIMIT 1")
        );

        String prompt = PromptTemplateFactory.buildAskPrompt(
                document.getContent(),
                documentPackage == null ? "" : documentPackage.getResultText(),
                request.getQuestion().trim()
        );
        String answer = aiService.chat(prompt);

        ChatRecord record = new ChatRecord();
        record.setUserId(request.getUserId());
        record.setDocumentId(request.getDocumentId());
        record.setUserQuestion(request.getQuestion().trim());
        record.setAiAnswer(answer);
        record.setCreateTime(LocalDateTime.now());
        chatRecordMapper.insert(record);

        log.info("文档追问成功，userId={}, documentId={}, chatRecordId={}",
                request.getUserId(), request.getDocumentId(), record.getId());
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
        if (documentMapper.selectById(documentId) == null) {
            throw new BusinessException("文档不存在");
        }
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
    }

    private void validateAskRequest(ChatAskRequest request) {
        if (request == null
                || request.getUserId() == null
                || request.getDocumentId() == null
                || !StringUtils.hasText(request.getQuestion())) {
            throw new BusinessException("userId、documentId 和问题不能为空");
        }
    }
}
