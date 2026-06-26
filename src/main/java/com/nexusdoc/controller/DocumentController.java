package com.nexusdoc.controller;

import com.nexusdoc.common.result.ApiResponse;
import com.nexusdoc.dto.DocumentGenerateRequest;
import com.nexusdoc.service.DocumentService;
import com.nexusdoc.vo.DocumentDetailVO;
import com.nexusdoc.vo.DocumentListVO;
import com.nexusdoc.vo.WebSearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/generate")
    public ApiResponse<DocumentDetailVO> generateDocument(@RequestBody DocumentGenerateRequest request) {
        return ApiResponse.success(documentService.generateDocument(request));
    }

    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateDocumentStream(@RequestBody DocumentGenerateRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> {
            try {
                sendEvent(emitter, "start", Map.of("type", "start", "status", "started"));
                DocumentDetailVO detail = documentService.generateDocumentStream(
                        request,
                        delta -> sendEvent(emitter, "delta", Map.of("type", "delta", "content", delta)),
                        source -> sendEvent(emitter, "source", buildSourceEvent(source)),
                        warning -> sendEvent(emitter, "warning", Map.of("type", "warning", "message", warning))
                );
                Map<String, Object> saved = new HashMap<>();
                saved.put("type", "saved");
                saved.put("documentId", detail.getDocumentId());
                saved.put("title", detail.getTitle());
                sendEvent(emitter, "saved", saved);

                Map<String, Object> done = new HashMap<>();
                done.put("type", "done");
                done.put("status", "done");
                done.put("documentId", detail.getDocumentId());
                done.put("title", detail.getTitle());
                done.put("resultText", detail.getResultText());
                sendEvent(emitter, "done", done);
            } catch (Exception exception) {
                log.warn("流式文档生成失败：{}", exception.getMessage());
                try {
                    sendEvent(emitter, "error", Map.of(
                            "type", "error",
                            "message", "AI 服务暂时不可用，请稍后重试"
                    ));
                } catch (RuntimeException sendException) {
                    log.debug("流式错误事件发送失败：{}", sendException.getMessage());
                }
            } finally {
                emitter.complete();
            }
        });
        return emitter;
    }

    @GetMapping("/list")
    public ApiResponse<List<DocumentListVO>> listDocuments(@RequestParam(required = false) Long userId) {
        return ApiResponse.success(documentService.listDocuments(userId));
    }

    @GetMapping("/detail/{documentId}")
    public ApiResponse<DocumentDetailVO> getDocumentDetail(@PathVariable Long documentId) {
        return ApiResponse.success(documentService.getDocumentDetail(documentId));
    }

    @DeleteMapping("/{documentId}")
    public ApiResponse<Void> deleteDocument(@PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
        return ApiResponse.success(null);
    }

    private Map<String, Object> buildSourceEvent(WebSearchResultVO source) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "source");
        data.put("title", source.getTitle());
        data.put("url", source.getUrl());
        data.put("snippet", source.getSnippet());
        data.put("source", source.getSource());
        return data;
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data, MediaType.APPLICATION_JSON));
        } catch (IOException exception) {
            throw new IllegalStateException("SSE 连接已关闭", exception);
        }
    }
}
