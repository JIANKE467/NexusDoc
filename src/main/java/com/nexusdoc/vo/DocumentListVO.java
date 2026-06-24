package com.nexusdoc.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentListVO {

    private Long documentId;

    private String title;

    private String docType;

    private String tag;

    private String summaryPreview;

    private LocalDateTime createTime;
}
