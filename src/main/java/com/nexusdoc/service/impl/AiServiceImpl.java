package com.nexusdoc.service.impl;

import com.nexusdoc.ai.SiliconFlowRequest;
import com.nexusdoc.ai.SiliconFlowResponse;
import com.nexusdoc.common.config.SiliconFlowProperties;
import com.nexusdoc.common.exception.BusinessException;
import com.nexusdoc.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final SiliconFlowProperties siliconFlowProperties;

    private final RestClient restClient = RestClient.create();

    @Override
    public String chat(String prompt) {
        if (!StringUtils.hasText(siliconFlowProperties.getApiKey())) {
            throw new BusinessException("未配置 SILICONFLOW_API_KEY，无法调用 AI 服务");
        }

        try {
            SiliconFlowRequest request = SiliconFlowRequest.userPrompt(
                    siliconFlowProperties.getModel(),
                    prompt
            );
            SiliconFlowResponse response = restClient.post()
                    .uri(siliconFlowProperties.getBaseUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + siliconFlowProperties.getApiKey())
                    .body(request)
                    .retrieve()
                    .body(SiliconFlowResponse.class);

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                log.warn("硅基流动 API 返回结果为空");
                throw new BusinessException("AI 返回结果为空，请稍后重试");
            }

            String content = response.getChoices().get(0).getMessage().getContent();
            if (!StringUtils.hasText(content)) {
                log.warn("硅基流动 API 返回内容为空");
                throw new BusinessException("AI 返回内容为空，请稍后重试");
            }
            return content;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("硅基流动 API 调用失败：{}", exception.getMessage());
            throw new BusinessException("AI 服务调用失败，请稍后重试");
        }
    }
}
