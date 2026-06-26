package com.nexusdoc.service;

import com.nexusdoc.dto.ChatAskRequest;
import com.nexusdoc.vo.ChatAnswerVO;
import com.nexusdoc.vo.ChatRecordVO;

import java.util.List;

public interface ChatService {

    ChatAnswerVO ask(ChatAskRequest request);

    List<ChatRecordVO> listRecords(Long documentId, String deviceId);
}
