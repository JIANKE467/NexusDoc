package com.nexusdoc.service;

import com.nexusdoc.vo.WebSearchResultVO;

import java.util.List;

public interface WebSearchService {

    /**
     * 根据用户问题搜索网络资料。
     *
     * @param query 搜索关键词或用户问题
     * @return 搜索结果列表
     */
    List<WebSearchResultVO> search(String query);
}
