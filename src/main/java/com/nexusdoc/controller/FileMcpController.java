package com.nexusdoc.controller;

import com.nexusdoc.common.result.ApiResponse;
import com.nexusdoc.service.FileMcpService;
import com.nexusdoc.vo.FileExtractResultVO;
import com.nexusdoc.vo.FileMcpCapabilitiesVO;
import com.nexusdoc.vo.FileMcpGenerateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/mcp/file")
@RequiredArgsConstructor
public class FileMcpController {

    private final FileMcpService fileMcpService;

    @GetMapping("/capabilities")
    public ApiResponse<FileMcpCapabilitiesVO> getCapabilities() {
        return ApiResponse.success(fileMcpService.getCapabilities());
    }

    @PostMapping("/parse")
    public ApiResponse<FileExtractResultVO> parseFile(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(fileMcpService.parseFile(file));
    }

    @PostMapping("/generate")
    public ApiResponse<FileMcpGenerateVO> generateFromFile(@RequestParam("file") MultipartFile file,
                                                           @RequestParam(required = false) Long userId,
                                                           @RequestParam(required = false) String mode,
                                                           @RequestParam(required = false) Boolean enableWebSearch,
                                                           @RequestParam(required = false) String cardTypes,
                                                           @RequestParam(required = false) String requirement) {
        return ApiResponse.success(fileMcpService.generateFromFile(
                file,
                userId,
                mode,
                enableWebSearch,
                cardTypes,
                requirement
        ));
    }
}
