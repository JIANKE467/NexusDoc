package com.nexusdoc.ai;

import lombok.Data;

import java.util.List;

@Data
public class SiliconFlowResponse {

    private List<Choice> choices;

    @Data
    public static class Choice {

        private SiliconFlowRequest.Message message;
    }
}
