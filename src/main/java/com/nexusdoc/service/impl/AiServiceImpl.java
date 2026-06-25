package com.nexusdoc.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusdoc.ai.SiliconFlowRequest;
import com.nexusdoc.ai.SiliconFlowResponse;
import com.nexusdoc.common.config.SiliconFlowProperties;
import com.nexusdoc.common.exception.BusinessException;
import com.nexusdoc.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final SiliconFlowProperties siliconFlowProperties;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generate(String prompt) {
        if (!StringUtils.hasText(siliconFlowProperties.getApiKey())) {
            throw new BusinessException("硅基流动 API Key 未配置，请检查 application-local.yml 或 SILICONFLOW_API_KEY");
        }
        if (!StringUtils.hasText(prompt)) {
            throw new BusinessException("AI Prompt 不能为空");
        }

        try {
            SiliconFlowRequest request = SiliconFlowRequest.userPrompt(
                    siliconFlowProperties.getModel(),
                    prompt,
                    siliconFlowProperties.getTemperature(),
                    siliconFlowProperties.getMaxTokens()
            );
            SiliconFlowResponse response = restClient.post()
                    .uri(siliconFlowProperties.getBaseUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + siliconFlowProperties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(SiliconFlowResponse.class);

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                log.warn("硅基流动 API 返回结果为空");
                throw new BusinessException("AI 生成结果为空，请稍后重试");
            }

            String content = response.getChoices().get(0).getMessage().getContent();
            if (!StringUtils.hasText(content)) {
                log.warn("硅基流动 API 返回内容为空");
                throw new BusinessException("AI 生成结果为空，请稍后重试");
            }
            return content;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            String responseMessage = extractSiliconFlowErrorMessage(exception);
            log.error("硅基流动 API 调用失败，status={}, body={}", exception.getStatusCode(), exception.getResponseBodyAsString());
            throw new BusinessException(responseMessage);
        } catch (Exception exception) {
            log.error("硅基流动 API 调用失败：{}", exception.getMessage());
            throw new BusinessException("AI 服务调用失败，请检查硅基流动 API 配置");
        }
    }

    private String extractSiliconFlowErrorMessage(RestClientResponseException exception) {
        String body = exception.getResponseBodyAsString();
        if (StringUtils.hasText(body)) {
            try {
                JsonNode root = objectMapper.readTree(body);
                JsonNode message = root.path("message");
                if (message.isTextual() && StringUtils.hasText(message.asText())) {
                    return "硅基流动 API 返回错误：" + message.asText();
                }
                JsonNode errorMessage = root.path("error").path("message");
                if (errorMessage.isTextual() && StringUtils.hasText(errorMessage.asText())) {
                    return "硅基流动 API 返回错误：" + errorMessage.asText();
                }
            } catch (Exception ignored) {
                log.debug("硅基流动错误响应不是 JSON：{}", body);
            }
        }
        if (exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403) {
            return "硅基流动 API 鉴权失败，请检查 API Key 是否正确";
        }
        if (exception.getStatusCode().value() == 404) {
            return "硅基流动 API 模型或接口不存在，请检查模型名和接口地址";
        }
        if (exception.getStatusCode().value() == 429) {
            return "硅基流动 API 调用频率或额度受限，请稍后重试";
        }
        return "硅基流动 API 调用失败，HTTP " + exception.getStatusCode().value();
    }
}
