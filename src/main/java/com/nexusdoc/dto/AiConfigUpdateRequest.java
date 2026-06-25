package com.nexusdoc.dto;

import lombok.Data;

@Data
public class AiConfigUpdateRequest {

    private String apiKey;

    private String baseUrl;

    private String model;
}
