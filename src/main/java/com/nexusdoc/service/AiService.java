package com.nexusdoc.service;

public interface AiService {

    /**
     * 根据 Prompt 调用 AI 生成文本结果。
     *
     * @param prompt 提示词
     * @return AI 生成结果
     */
    String generate(String prompt);

    default String chat(String prompt) {
        return generate(prompt);
    }
}
