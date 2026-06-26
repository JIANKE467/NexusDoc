package com.nexusdoc.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "nexusdoc.file-mcp")
public class FileMcpProperties {

    private Boolean enabled = true;

    private Integer maxFileSizeMb = 10;

    private Integer maxExtractChars = 20000;

    private Integer maxPdfPages = 80;

    private List<String> supportedTypes = new ArrayList<>(List.of(
            "txt", "md", "markdown", "csv", "json", "docx", "pdf"
    ));
}
