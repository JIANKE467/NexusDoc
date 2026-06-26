package com.nexusdoc.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FileExtractResultVO {

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String textPreview;

    private Integer originalTextLength;

    private Integer usedTextLength;

    private Boolean truncated;

    private Integer pageCount;

    private Integer parsedPageCount;

    private String parserName;

    private List<String> warnings;

    private String extractedText;
}
