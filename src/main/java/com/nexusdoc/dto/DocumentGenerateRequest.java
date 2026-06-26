package com.nexusdoc.dto;

import lombok.Data;

@Data
public class DocumentGenerateRequest {

    private Long userId;

    private String deviceId;

    private String title;

    private String docType;

    private String tag;

    private String content;

    private Boolean enableWebSearch;
}
