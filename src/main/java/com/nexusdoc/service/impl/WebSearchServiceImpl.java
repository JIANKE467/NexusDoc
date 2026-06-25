package com.nexusdoc.service.impl;

import com.nexusdoc.common.config.WebSearchProperties;
import com.nexusdoc.common.exception.BusinessException;
import com.nexusdoc.search.SerpApiSearchResponse;
import com.nexusdoc.service.WebSearchService;
import com.nexusdoc.vo.WebSearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSearchServiceImpl implements WebSearchService {

    private static final int DEFAULT_NUM = 5;
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final int DEFAULT_CACHE_TTL_SECONDS = 600;

    private final WebSearchProperties webSearchProperties;

    private final Map<String, CacheEntry> searchCache = new ConcurrentHashMap<>();

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
            String normalizedQuery = query.trim();
            String cacheKey = buildCacheKey(normalizedQuery);
            CacheEntry cacheEntry = searchCache.get(cacheKey);
            if (cacheEntry != null && !cacheEntry.isExpired()) {
                log.info("联网搜索命中缓存，resultCount={}", cacheEntry.results().size());
                return cacheEntry.results();
            }

            log.info("开始执行联网搜索，provider={}", webSearchProperties.getProvider());
            SerpApiSearchResponse response = buildRestClient()
                    .get()
                    .uri(buildSearchUri(normalizedQuery))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(SerpApiSearchResponse.class);
            List<WebSearchResultVO> results = parseResults(response);
            searchCache.put(cacheKey, new CacheEntry(List.copyOf(results),
                    Instant.now().plusSeconds(cacheTtlSeconds())));
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
        return UriComponentsBuilder.fromUriString(webSearchProperties.getBaseUrl().trim())
                .queryParam("engine", defaultText(webSearchProperties.getEngine(), "google"))
                .queryParam("q", query)
                .queryParam("api_key", webSearchProperties.getApiKey())
                .queryParam("hl", defaultText(webSearchProperties.getHl(), "zh-cn"))
                .queryParam("gl", defaultText(webSearchProperties.getGl(), "cn"))
                .queryParam("num", num())
                .build()
                .encode()
                .toUri();
    }

    private List<WebSearchResultVO> parseResults(SerpApiSearchResponse response) {
        if (response == null || response.getOrganicResults() == null
                || response.getOrganicResults().isEmpty()) {
            return List.of();
        }

        List<WebSearchResultVO> results = new ArrayList<>();
        int index = 1;
        for (SerpApiSearchResponse.OrganicResult item : response.getOrganicResults()) {
            WebSearchResultVO result = toResult(item, index);
            if (StringUtils.hasText(result.getTitle()) || StringUtils.hasText(result.getSnippet())) {
                results.add(result);
                index++;
            }
            if (results.size() >= num()) {
                break;
            }
        }
        return results;
    }

    private WebSearchResultVO toResult(SerpApiSearchResponse.OrganicResult item, int index) {
        WebSearchResultVO result = new WebSearchResultVO();
        result.setIndex(item.getPosition() == null ? index : item.getPosition());
        result.setTitle(blankToEmpty(item.getTitle()));
        result.setUrl(blankToEmpty(item.getLink()));
        result.setSnippet(blankToEmpty(item.getSnippet()));
        result.setSource(blankToEmpty(item.getSource()));
        return result;
    }

    private void validateConfig() {
        if (!StringUtils.hasText(webSearchProperties.getApiKey())) {
            throw new BusinessException("SerpApi API Key 未配置，请检查后端运行环境变量 SERPAPI_API_KEY");
        }
        if (!StringUtils.hasText(webSearchProperties.getBaseUrl())) {
            throw new BusinessException("SerpApi 搜索接口地址未配置，请检查 web-search.base-url");
        }
    }

    private int num() {
        Integer num = webSearchProperties.getNum();
        return num == null || num <= 0 ? DEFAULT_NUM : num;
    }

    private int timeoutSeconds() {
        Integer timeoutSeconds = webSearchProperties.getTimeoutSeconds();
        return timeoutSeconds == null || timeoutSeconds <= 0
                ? DEFAULT_TIMEOUT_SECONDS
                : timeoutSeconds;
    }

    private int cacheTtlSeconds() {
        Integer cacheTtlSeconds = webSearchProperties.getCacheTtlSeconds();
        return cacheTtlSeconds == null || cacheTtlSeconds <= 0
                ? DEFAULT_CACHE_TTL_SECONDS
                : cacheTtlSeconds;
    }

    private String buildCacheKey(String query) {
        return String.join("|",
                defaultText(webSearchProperties.getProvider(), "serpapi"),
                defaultText(webSearchProperties.getBaseUrl(), ""),
                defaultText(webSearchProperties.getEngine(), "google"),
                defaultText(webSearchProperties.getHl(), "zh-cn"),
                defaultText(webSearchProperties.getGl(), "cn"),
                String.valueOf(num()),
                query.toLowerCase());
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record CacheEntry(List<WebSearchResultVO> results, Instant expireAt) {

        private boolean isExpired() {
            return Instant.now().isAfter(expireAt);
        }
    }
}
