package com.nexusdoc.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusdoc.common.config.WebSearchProperties;
import com.nexusdoc.common.exception.BusinessException;
import com.nexusdoc.service.WebSearchService;
import com.nexusdoc.vo.WebSearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSearchServiceImpl implements WebSearchService {

    private static final int DEFAULT_MAX_RESULTS = 5;
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;

    private final WebSearchProperties webSearchProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<WebSearchResultVO> search(String query) {
        if (!Boolean.TRUE.equals(webSearchProperties.getEnabled())) {
            return List.of();
        }
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        validateConfig();

        try {
            log.info("开始执行联网搜索，provider={}", webSearchProperties.getProvider());
            String responseBody = buildRestClient()
                    .get()
                    .uri(buildSearchUri(query.trim()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + webSearchProperties.getApiKey())
                    .header("X-API-Key", webSearchProperties.getApiKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
            List<WebSearchResultVO> results = parseResults(responseBody);
            log.info("联网搜索完成，resultCount={}", results.size());
            return results;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("联网搜索失败，已降级为普通 AI 回答：{}", exception.getMessage());
            return List.of();
        }
    }

    private RestClient buildRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds()));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private URI buildSearchUri(String query) {
        String baseUrl = webSearchProperties.getBaseUrl().trim();
        if (baseUrl.contains("{query}")) {
            return URI.create(baseUrl.replace("{query}", UriComponentsBuilder
                    .fromPath(query).build().encode().toUriString()));
        }
        return UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("q", query)
                .queryParam("query", query)
                .queryParam("count", maxResults())
                .queryParam("max_results", maxResults())
                .build(true)
                .toUri();
    }

    private List<WebSearchResultVO> parseResults(String responseBody) throws Exception {
        if (!StringUtils.hasText(responseBody)) {
            return List.of();
        }
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode resultArray = firstArray(root,
                "/results",
                "/items",
                "/data",
                "/organic_results",
                "/webPages/value");
        if (resultArray == null || !resultArray.isArray()) {
            return List.of();
        }

        List<WebSearchResultVO> results = new ArrayList<>();
        int index = 1;
        for (JsonNode item : resultArray) {
            WebSearchResultVO result = toResult(item, index);
            if (StringUtils.hasText(result.getTitle()) || StringUtils.hasText(result.getSnippet())) {
                results.add(result);
                index++;
            }
            if (results.size() >= maxResults()) {
                break;
            }
        }
        return results;
    }

    private WebSearchResultVO toResult(JsonNode item, int index) {
        WebSearchResultVO result = new WebSearchResultVO();
        result.setIndex(index);
        result.setTitle(firstText(item, "title", "name", "heading"));
        result.setUrl(firstText(item, "url", "link", "href"));
        result.setSnippet(firstText(item, "snippet", "description", "content", "summary"));
        result.setSource(firstText(item, "source", "site", "displayLink"));
        return result;
    }

    private JsonNode firstArray(JsonNode root, String... pointers) {
        for (String pointer : pointers) {
            JsonNode node = root.at(pointer);
            if (node != null && node.isArray()) {
                return node;
            }
        }
        return null;
    }

    private String firstText(JsonNode item, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = item.path(fieldName);
            if (value.isTextual() && StringUtils.hasText(value.asText())) {
                return value.asText();
            }
        }
        return "";
    }

    private void validateConfig() {
        if (!StringUtils.hasText(webSearchProperties.getApiKey())) {
            throw new BusinessException("网络搜索 API Key 未配置，请检查后端运行环境变量 WEB_SEARCH_API_KEY");
        }
        if (!StringUtils.hasText(webSearchProperties.getBaseUrl())) {
            throw new BusinessException("网络搜索接口地址未配置，请检查 web-search.base-url");
        }
    }

    private int maxResults() {
        Integer maxResults = webSearchProperties.getMaxResults();
        return maxResults == null || maxResults <= 0 ? DEFAULT_MAX_RESULTS : maxResults;
    }

    private int timeoutSeconds() {
        Integer timeoutSeconds = webSearchProperties.getTimeoutSeconds();
        return timeoutSeconds == null || timeoutSeconds <= 0
                ? DEFAULT_TIMEOUT_SECONDS
                : timeoutSeconds;
    }
}
