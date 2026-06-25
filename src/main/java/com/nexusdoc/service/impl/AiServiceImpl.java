package com.nexusdoc.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusdoc.ai.SiliconFlowChatRequest;
import com.nexusdoc.ai.SiliconFlowChatResponse;
import com.nexusdoc.common.config.SiliconFlowProperties;
import com.nexusdoc.common.exception.BusinessException;
import com.nexusdoc.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final SiliconFlowProperties siliconFlowProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestClient restClient = RestClient.builder()
            .requestFactory(new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(20))
                    .version(HttpClient.Version.HTTP_1_1)
                    .build()))
            .build();

    @Override
    public String generate(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            throw new BusinessException("AI 提示词不能为空");
        }
        validateConfig();

        try {
            log.info("开始调用 AI 服务，model={}", siliconFlowProperties.getModel());
            SiliconFlowChatRequest request = buildRequest(prompt);
            SiliconFlowChatResponse response = restClient.post()
                    .uri(siliconFlowProperties.getBaseUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + siliconFlowProperties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(SiliconFlowChatResponse.class);

            String content = extractContent(response);
            log.info("AI 服务调用成功");
            return content;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            log.error("AI 服务调用失败，status={}", exception.getStatusCode());
            throw new BusinessException("AI 服务暂时不可用，请稍后重试");
        } catch (Exception exception) {
            log.warn("AI 服务 Java HTTP 调用失败，尝试兼容请求方式：{}", exception.getMessage());
            return generateWithCurlFallback(prompt);
        }
    }

    private SiliconFlowChatRequest buildRequest(String prompt) {
        return SiliconFlowChatRequest.userPrompt(
                siliconFlowProperties.getModel(),
                prompt,
                siliconFlowProperties.getTemperature(),
                siliconFlowProperties.getMaxTokens(),
                siliconFlowProperties.getStream()
        );
    }

    private String generateWithCurlFallback(String prompt) {
        Path requestBodyPath = null;
        try {
            requestBodyPath = Files.createTempFile("nexusdoc-ai-request-", ".json");
            Files.writeString(requestBodyPath, objectMapper.writeValueAsString(buildRequest(prompt)),
                    StandardCharsets.UTF_8);

            ProcessBuilder processBuilder = new ProcessBuilder(List.of(
                    "curl",
                    "-sS",
                    "--connect-timeout",
                    "20",
                    "--max-time",
                    "90",
                    "-w",
                    "\\n%{http_code}",
                    "-K",
                    "-",
                    "--data-binary",
                    "@" + requestBodyPath
            ));
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            String curlConfig = """
                    url = "%s"
                    request = "POST"
                    header = "Content-Type: application/json"
                    header = "Accept: application/json"
                    header = "Authorization: Bearer %s"
                    """.formatted(siliconFlowProperties.getBaseUrl(),
                    siliconFlowProperties.getApiKey());
            process.getOutputStream().write(curlConfig.getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().close();

            boolean completed = process.waitFor(90, TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!completed) {
                process.destroyForcibly();
                throw new BusinessException("AI 服务暂时不可用，请稍后重试");
            }
            return parseCurlResponse(output);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("AI 服务兼容请求方式调用失败：{}", exception.getMessage());
            throw new BusinessException("AI 服务暂时不可用，请稍后重试");
        } finally {
            deleteTempFile(requestBodyPath);
        }
    }

    private String parseCurlResponse(String output) throws IOException {
        if (!StringUtils.hasText(output)) {
            throw new BusinessException("AI 服务暂时不可用，请稍后重试");
        }
        int splitIndex = output.lastIndexOf('\n');
        if (splitIndex < 0) {
            throw new BusinessException("AI 服务暂时不可用，请稍后重试");
        }
        String body = output.substring(0, splitIndex).trim();
        String status = output.substring(splitIndex + 1).trim();
        if (!status.startsWith("2")) {
            log.error("AI 服务兼容请求方式调用失败，status={}", status);
            throw new BusinessException("AI 服务暂时不可用，请稍后重试");
        }
        String content = extractContent(objectMapper.readValue(body, SiliconFlowChatResponse.class));
        log.info("AI 服务调用成功");
        return content;
    }

    private void deleteTempFile(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.debug("AI 临时请求文件清理失败：{}", exception.getMessage());
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(siliconFlowProperties.getApiKey())) {
            throw new BusinessException("AI API Key 未配置，请检查后端运行环境变量 SILICONFLOW_API_KEY");
        }
        if (!StringUtils.hasText(siliconFlowProperties.getBaseUrl())) {
            throw new BusinessException("AI 接口地址未配置，请检查 siliconflow.base-url");
        }
        if (!StringUtils.hasText(siliconFlowProperties.getModel())) {
            throw new BusinessException("AI 模型名称未配置，请检查 siliconflow.model");
        }
    }

    private String extractContent(SiliconFlowChatResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new BusinessException("AI 生成结果为空，请稍后重试");
        }
        SiliconFlowChatResponse.Choice choice = response.getChoices().get(0);
        if (choice == null || choice.getMessage() == null
                || !StringUtils.hasText(choice.getMessage().getContent())) {
            throw new BusinessException("AI 生成结果为空，请稍后重试");
        }
        return choice.getMessage().getContent();
    }
}
