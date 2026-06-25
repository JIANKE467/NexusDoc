package com.nexusdoc.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiliconFlowRequest {

    private String model;

    private List<Message> messages;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Double temperature;

    private Boolean stream;

    private Integer n;

    public static SiliconFlowRequest userPrompt(
            String model,
            String prompt,
            Double temperature,
            Integer maxTokens
    ) {
        SiliconFlowRequest request = new SiliconFlowRequest();
        request.setModel(model);
        request.setMessages(List.of(
                new Message("system", "你是文枢 NexusDoc 的 AI 文档理解与知识整理助手，请用中文输出结构清晰、可执行的结果。"),
                new Message("user", prompt)
        ));
        request.setMaxTokens(maxTokens);
        request.setTemperature(temperature);
        request.setStream(false);
        request.setN(1);
        return request;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {

        private String role;

        private String content;
    }
}
