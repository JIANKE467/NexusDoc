package com.nexusdoc.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FileMcpCapabilitiesVO {

    private Boolean enabled;

    private String name;

    private String description;

    private Integer maxFileSizeMb;

    private Integer maxExtractChars;

    private Integer maxPdfPages;

    private List<String> supportedTypes;
}
