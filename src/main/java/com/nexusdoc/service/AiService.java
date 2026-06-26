package com.nexusdoc.service;

import java.util.function.Consumer;

public interface AiService {

    /**
     * 根据 Prompt 调用 AI 生成文本结果。
     *
     * @param prompt 提示词
     * @return AI 生成结果
     */
    String generate(String prompt);

    /**
     * 根据 Prompt 调用 AI 流式生成文本结果。
     * 如果上游流式不可用，实现层可以回退为普通生成并一次性回调完整结果。
     *
     * @param prompt 提示词
     * @param onDelta 增量文本回调
     * @return AI 完整生成结果
     */
    String streamGenerate(String prompt, Consumer<String> onDelta);
}
