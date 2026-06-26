package com.nexusdoc.controller;

import com.nexusdoc.common.config.DeepSeekProperties;
import com.nexusdoc.common.config.SiliconFlowProperties;
import com.nexusdoc.common.result.ApiResponse;
import com.nexusdoc.dto.AiTestRequest;
import com.nexusdoc.service.AiService;
import com.nexusdoc.vo.AiConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final DeepSeekProperties deepSeekProperties;
    private final SiliconFlowProperties siliconFlowProperties;

    @GetMapping("/config")
    public ApiResponse<AiConfigVO> getConfig() {
        return ApiResponse.success(AiConfigVO.builder()
                .provider(StringUtils.hasText(deepSeekProperties.getApiKey())
                        ? "DeepSeek"
                        : "SiliconFlow")
                .baseUrl(StringUtils.hasText(deepSeekProperties.getApiKey())
                        ? deepSeekProperties.getBaseUrl()
                        : siliconFlowProperties.getBaseUrl())
                .model(StringUtils.hasText(deepSeekProperties.getApiKey())
                        ? deepSeekProperties.getModel()
                        : siliconFlowProperties.getModel())
                .apiKeyConfigured(StringUtils.hasText(deepSeekProperties.getApiKey())
                        || StringUtils.hasText(siliconFlowProperties.getApiKey()))
                .build());
    }

    @PostMapping("/test")
    public ApiResponse<String> test(@RequestBody AiTestRequest request) {
        String prompt = request != null && StringUtils.hasText(request.getPrompt())
                ? request.getPrompt()
                : "请用一句话回复：文枢 NexusDoc AI 已接入。";
        return ApiResponse.success(aiService.generate(prompt));
    }
}
