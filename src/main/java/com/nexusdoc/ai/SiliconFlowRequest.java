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

    private Boolean stream = false;

    @JsonProperty("max_tokens")
    private Integer maxTokens = 2048;

    private Double temperature = 0.3;

    public static SiliconFlowRequest userPrompt(String model, String prompt) {
        SiliconFlowRequest request = new SiliconFlowRequest();
        request.setModel(model);
        request.setMessages(List.of(new Message("user", prompt)));
        request.setStream(false);
        request.setMaxTokens(2048);
        request.setTemperature(0.3);
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
