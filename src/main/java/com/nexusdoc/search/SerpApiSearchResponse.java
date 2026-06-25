package com.nexusdoc.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SerpApiSearchResponse {

    @JsonProperty("organic_results")
    private List<OrganicResult> organicResults;

    @Data
    public static class OrganicResult {

        private Integer position;

        private String title;

        private String link;

        private String snippet;

        private String source;
    }
}
