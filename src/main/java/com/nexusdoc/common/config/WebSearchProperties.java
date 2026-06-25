package com.nexusdoc.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "web-search")
public class WebSearchProperties {

    private Boolean enabled;

    private String apiKey;

    private String baseUrl;

    private String provider;

    private Integer maxResults;

    private Integer timeoutSeconds;
}
