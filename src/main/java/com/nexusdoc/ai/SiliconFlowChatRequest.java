package com.nexusdoc.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiliconFlowChatRequest {

    private static final String SYSTEM_PROMPT = """
            你是文枢 NexusDoc 的 AI 文档理解与知识整理助手。你擅长文档总结、\
            结构化提取、思维导图数据生成、小说设定生成和趋势问题分析。\
            请严格按照用户要求输出，不要编造原文没有的信息。
            """;

    private String model;

    private List<Message> messages;

    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Boolean stream;

    public static SiliconFlowChatRequest userPrompt(
            String model,
            String prompt,
            Double temperature,
            Integer maxTokens,
            Boolean stream
    ) {
        SiliconFlowChatRequest request = new SiliconFlowChatRequest();
        request.setModel(model);
        request.setMessages(List.of(
                new Message("system", SYSTEM_PROMPT),
                new Message("user", prompt)
        ));
        request.setTemperature(temperature);
        request.setMaxTokens(maxTokens);
        request.setStream(Boolean.TRUE.equals(stream));
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
