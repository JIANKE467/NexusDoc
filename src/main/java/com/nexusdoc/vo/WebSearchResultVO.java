package com.nexusdoc.vo;

import lombok.Data;

@Data
public class WebSearchResultVO {

    private String title;

    private String url;

    private String snippet;

    private String source;

    private Integer index;
}
