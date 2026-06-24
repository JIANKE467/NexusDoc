package com.nexusdoc.controller;

import com.nexusdoc.common.result.ApiResponse;
import com.nexusdoc.dto.ChatAskRequest;
import com.nexusdoc.service.ChatService;
import com.nexusdoc.vo.ChatAnswerVO;
import com.nexusdoc.vo.ChatRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/ask")
    public ApiResponse<ChatAnswerVO> ask(@RequestBody ChatAskRequest request) {
        return ApiResponse.success(chatService.ask(request));
    }

    @GetMapping("/list")
    public ApiResponse<List<ChatRecordVO>> listRecords(@RequestParam Long documentId) {
        return ApiResponse.success(chatService.listRecords(documentId));
    }
}
