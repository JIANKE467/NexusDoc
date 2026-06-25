package com.nexusdoc.controller;

import com.nexusdoc.common.config.SiliconFlowProperties;
import com.nexusdoc.common.exception.BusinessException;
import com.nexusdoc.common.result.ApiResponse;
import com.nexusdoc.dto.AiConfigUpdateRequest;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final SiliconFlowProperties siliconFlowProperties;

    @GetMapping("/config")
    public ApiResponse<AiConfigVO> getConfig() {
        return ApiResponse.success(AiConfigVO.builder()
                .provider("SiliconFlow")
                .baseUrl(siliconFlowProperties.getBaseUrl())
                .model(siliconFlowProperties.getModel())
                .apiKeyConfigured(StringUtils.hasText(siliconFlowProperties.getApiKey()))
                .build());
    }

    @PostMapping("/test")
    public ApiResponse<String> test(@RequestBody AiTestRequest request) {
        String prompt = request != null && StringUtils.hasText(request.getPrompt())
                ? request.getPrompt()
                : "请用一句话回复：文枢 NexusDoc AI 已接入。";
        return ApiResponse.success(aiService.generate(prompt));
    }

    @PostMapping("/config")
    public ApiResponse<AiConfigVO> updateConfig(@RequestBody AiConfigUpdateRequest request) {
        if (request == null || !StringUtils.hasText(request.getApiKey())) {
            throw new BusinessException("请填写硅基流动 API Key");
        }

        String baseUrl = StringUtils.hasText(request.getBaseUrl())
                ? request.getBaseUrl().trim()
                : "https://api.siliconflow.cn/v1/chat/completions";
        String model = StringUtils.hasText(request.getModel())
                ? request.getModel().trim()
                : "Qwen/Qwen3-8B";

        siliconFlowProperties.setApiKey(request.getApiKey().trim());
        siliconFlowProperties.setBaseUrl(baseUrl);
        siliconFlowProperties.setModel(model);

        persistLocalConfig();
        return getConfig();
    }

    private void persistLocalConfig() {
        String content = """
                siliconflow:
                  api-key: '%s'
                  base-url: '%s'
                  model: '%s'
                  temperature: %s
                  max-tokens: %s
                """.formatted(
                yamlQuote(siliconFlowProperties.getApiKey()),
                yamlQuote(siliconFlowProperties.getBaseUrl()),
                yamlQuote(siliconFlowProperties.getModel()),
                siliconFlowProperties.getTemperature(),
                siliconFlowProperties.getMaxTokens()
        );
        try {
            Files.writeString(Path.of("application-local.yml"), content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new BusinessException("保存本地 AI 配置失败：" + exception.getMessage());
        }
    }

    private String yamlQuote(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}
