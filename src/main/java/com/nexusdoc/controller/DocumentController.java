package com.nexusdoc.controller;

import com.nexusdoc.common.result.ApiResponse;
import com.nexusdoc.dto.DocumentGenerateRequest;
import com.nexusdoc.service.DocumentService;
import com.nexusdoc.vo.DocumentDetailVO;
import com.nexusdoc.vo.DocumentListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/generate")
    public ApiResponse<DocumentDetailVO> generateDocument(@RequestBody DocumentGenerateRequest request) {
        return ApiResponse.success(documentService.generateDocument(request));
    }

    @GetMapping("/list")
    public ApiResponse<List<DocumentListVO>> listDocuments(@RequestParam Long userId) {
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
}
