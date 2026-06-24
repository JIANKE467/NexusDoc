package com.nexusdoc.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentDetailVO {

    private Long documentId;

    private Long userId;

    private String title;

    private String docType;

    private String tag;

    private String content;

    private String resultText;

    private LocalDateTime createTime;
}
