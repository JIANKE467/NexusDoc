package com.nexusdoc.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiConfigVO {

    private String provider;

    private String baseUrl;

    private String model;

    private Boolean apiKeyConfigured;
}
