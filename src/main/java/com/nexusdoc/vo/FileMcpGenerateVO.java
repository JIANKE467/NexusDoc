package com.nexusdoc.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FileMcpGenerateVO {

    private String fileName;

    private String fileType;

    private Long fileSize;

    private Integer originalTextLength;

    private Integer usedTextLength;

    private Boolean truncated;

    private Integer pageCount;

    private Integer parsedPageCount;

    private List<String> warnings;

    private Long documentId;

    private String title;

    private String content;

    private String resultText;

    private DocumentDetailVO document;
}
