package com.nexusdoc.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.nexusdoc.ai.SiliconFlowChatRequest;
import com.nexusdoc.ai.SiliconFlowChatResponse;
import com.nexusdoc.common.config.DeepSeekProperties;
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
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final DeepSeekProperties deepSeekProperties;

    private final SiliconFlowProperties siliconFlowProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestClient restClient = RestClient.builder()
            .requestFactory(new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(20))
                    .version(HttpClient.Version.HTTP_1_1)
                    .build()))
            .build();

    private final HttpClient streamHttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    @Override
    public String generate(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            throw new BusinessException("AI 提示词不能为空");
        }
        List<AiProviderConfig> providers = buildProviderConfigs();
        if (providers.isEmpty()) {
            throw new BusinessException("AI API Key 未配置，请检查后端运行环境变量 DEEPSEEK_API_KEY 或 SILICONFLOW_API_KEY");
        }

        BusinessException lastBusinessException = null;
        for (AiProviderConfig provider : providers) {
            try {
                return generateWithProvider(prompt, provider);
            } catch (BusinessException exception) {
                lastBusinessException = exception;
                log.warn("AI 服务调用失败，provider={}，尝试下一个可用模型", provider.name());
            }
        }
        if (lastBusinessException != null) {
            throw lastBusinessException;
        }
        throw new BusinessException("AI 服务暂时不可用，请稍后重试");
    }

    @Override
    public String streamGenerate(String prompt, Consumer<String> onDelta) {
        if (!StringUtils.hasText(prompt)) {
            throw new BusinessException("AI 提示词不能为空");
        }
        List<AiProviderConfig> providers = buildProviderConfigs();
        if (providers.isEmpty()) {
            throw new BusinessException("AI API Key 未配置，请检查后端运行环境变量 DEEPSEEK_API_KEY 或 SILICONFLOW_API_KEY");
        }

        BusinessException lastBusinessException = null;
        for (AiProviderConfig provider : providers) {
            try {
                return streamGenerateWithProvider(prompt, provider, onDelta);
            } catch (BusinessException exception) {
                lastBusinessException = exception;
                log.warn("AI 流式调用失败，provider={}，尝试下一个可用模型", provider.name());
            }
        }
        if (lastBusinessException != null) {
            log.warn("AI 流式调用不可用，回退普通生成：{}", lastBusinessException.getMessage());
            String fallback = generate(prompt);
            if (StringUtils.hasText(fallback) && onDelta != null) {
                onDelta.accept(fallback);
            }
            return fallback;
        }
        throw new BusinessException("AI 服务暂时不可用，请稍后重试");
    }

    private String streamGenerateWithProvider(String prompt, AiProviderConfig provider, Consumer<String> onDelta) {
        validateProviderConfig(provider);
        try {
            log.info("开始调用 AI 流式服务，provider={}，model={}", provider.name(), provider.model());
            SiliconFlowChatRequest request = SiliconFlowChatRequest.userPrompt(
                    provider.model(),
                    prompt,
                    provider.temperature(),
                    provider.maxTokens(),
                    true
            );
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(provider.baseUrl()))
                    .timeout(Duration.ofSeconds(120))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + provider.apiKey())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<InputStream> response = streamHttpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("AI 流式服务调用失败，provider={}，status={}", provider.name(), response.statusCode());
                throw new BusinessException("AI 服务暂时不可用，请稍后重试");
            }
            String content = parseStreamResponse(response.body(), onDelta);
            if (!StringUtils.hasText(content)) {
                throw new BusinessException("AI 生成结果为空，请稍后重试");
            }
            log.info("AI 流式服务调用成功");
            return content;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("AI 流式服务调用异常，provider={}：{}", provider.name(), exception.getMessage());
            throw new BusinessException("AI 服务暂时不可用，请稍后重试");
        }
    }

    private String parseStreamResponse(InputStream inputStream, Consumer<String> onDelta) throws IOException {
        StringBuilder fullText = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("data:")) {
                    continue;
                }
                String data = trimmed.substring(5).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }
                String delta = extractStreamDelta(data);
                if (StringUtils.hasText(delta)) {
                    fullText.append(delta);
                    if (onDelta != null) {
                        onDelta.accept(delta);
                    }
                }
            }
        }
        return fullText.toString();
    }

    private String extractStreamDelta(String data) {
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode choice = root.path("choices").path(0);
            JsonNode delta = choice.path("delta").path("content");
            if (!delta.isMissingNode() && delta.isTextual()) {
                return delta.asText();
            }
            JsonNode message = choice.path("message").path("content");
            if (!message.isMissingNode() && message.isTextual()) {
                return message.asText();
            }
            return "";
        } catch (Exception exception) {
            log.debug("AI 流式片段解析失败：{}", exception.getMessage());
            return "";
        }
    }

    private String generateWithProvider(String prompt, AiProviderConfig provider) {
        validateProviderConfig(provider);
        try {
            log.info("开始调用 AI 服务，provider={}，model={}", provider.name(), provider.model());
            SiliconFlowChatRequest request = buildRequest(prompt, provider);
            SiliconFlowChatResponse response = restClient.post()
                    .uri(provider.baseUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + provider.apiKey())
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
            log.error("AI 服务调用失败，provider={}，status={}", provider.name(), exception.getStatusCode());
            throw new BusinessException("AI 服务暂时不可用，请稍后重试");
        } catch (Exception exception) {
            log.warn("AI 服务 Java HTTP 调用失败，provider={}，尝试兼容请求方式：{}",
                    provider.name(), exception.getMessage());
            return generateWithCurlFallback(prompt, provider);
        }
    }

    private SiliconFlowChatRequest buildRequest(String prompt, AiProviderConfig provider) {
        return SiliconFlowChatRequest.userPrompt(
                provider.model(),
                prompt,
                provider.temperature(),
                provider.maxTokens(),
                provider.stream()
        );
    }

    private String generateWithCurlFallback(String prompt, AiProviderConfig provider) {
        Path requestBodyPath = null;
        try {
            requestBodyPath = Files.createTempFile("nexusdoc-ai-request-", ".json");
            Files.writeString(requestBodyPath, objectMapper.writeValueAsString(buildRequest(prompt, provider)),
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
                    """.formatted(provider.baseUrl(), provider.apiKey());
            process.getOutputStream().write(curlConfig.getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().close();

            boolean completed = process.waitFor(90, TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!completed) {
                process.destroyForcibly();
                throw new BusinessException("AI 服务暂时不可用，请稍后重试");
            }
            return parseCurlResponse(output, provider);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("AI 服务兼容请求方式调用失败，provider={}：{}", provider.name(), exception.getMessage());
            throw new BusinessException("AI 服务暂时不可用，请稍后重试");
        } finally {
            deleteTempFile(requestBodyPath);
        }
    }

    private String parseCurlResponse(String output, AiProviderConfig provider) throws IOException {
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
            log.error("AI 服务兼容请求方式调用失败，provider={}，status={}", provider.name(), status);
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

    private List<AiProviderConfig> buildProviderConfigs() {
        return List.of(
                new AiProviderConfig(
                        "DeepSeek",
                        deepSeekProperties.getApiKey(),
                        normalizeChatCompletionsUrl(deepSeekProperties.getBaseUrl()),
                        deepSeekProperties.getModel(),
                        deepSeekProperties.getTemperature(),
                        deepSeekProperties.getMaxTokens(),
                        deepSeekProperties.getStream()
                ),
                new AiProviderConfig(
                        "SiliconFlow",
                        siliconFlowProperties.getApiKey(),
                        normalizeChatCompletionsUrl(siliconFlowProperties.getBaseUrl()),
                        siliconFlowProperties.getModel(),
                        siliconFlowProperties.getTemperature(),
                        siliconFlowProperties.getMaxTokens(),
                        siliconFlowProperties.getStream()
                )
        ).stream().filter(provider -> StringUtils.hasText(provider.apiKey())).toList();
    }

    private void validateProviderConfig(AiProviderConfig provider) {
        if (!StringUtils.hasText(provider.apiKey())) {
            throw new BusinessException("AI API Key 未配置，请检查后端运行环境变量");
        }
        if (!StringUtils.hasText(provider.baseUrl())) {
            throw new BusinessException("AI 接口地址未配置，请检查后端 AI base-url 配置");
        }
        if (!StringUtils.hasText(provider.model())) {
            throw new BusinessException("AI 模型名称未配置，请检查后端 AI model 配置");
        }
    }

    private String normalizeChatCompletionsUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return baseUrl;
        }
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/chat/completions")) {
            return trimmed;
        }
        return trimmed.replaceAll("/+$", "") + "/chat/completions";
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

    private record AiProviderConfig(
            String name,
            String apiKey,
            String baseUrl,
            String model,
            Double temperature,
            Integer maxTokens,
            Boolean stream) {
    }
}
