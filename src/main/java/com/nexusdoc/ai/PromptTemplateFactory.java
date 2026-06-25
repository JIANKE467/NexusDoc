package com.nexusdoc.ai;

import com.nexusdoc.vo.WebSearchResultVO;

import java.util.List;

public final class PromptTemplateFactory {

    private static final String FREE_CHAT = "智能回答";
    private static final String GENERAL_SUMMARY = "通用总结";
    private static final String MIND_MAP = "思维导图";
    private static final String TREND_ANALYSIS = "趋势与隐藏问题分析";

    private PromptTemplateFactory() {
    }

    public static String buildDocumentPrompt(String docType, String content) {
        return buildDocumentPrompt(docType, content, false, List.of());
    }

    public static String buildDocumentPrompt(String docType, String content,
                                             boolean webSearchAttempted,
                                             List<WebSearchResultVO> searchResults) {
        String normalizedType = normalizeDocType(docType);
        String trimmedContent = content == null ? "" : content.trim();
        if (isMindMapMode(normalizedType, trimmedContent)) {
            return buildMindMapPrompt(trimmedContent, webSearchAttempted, searchResults);
        }
        if (isTrendMode(normalizedType, trimmedContent)) {
            return buildTrendPrompt(trimmedContent, webSearchAttempted, searchResults);
        }
        if (isStructuredDocumentMode(normalizedType) && !isCreativeIntent(trimmedContent)) {
            return buildStructuredDocumentPrompt(normalizedType, trimmedContent,
                    webSearchAttempted, searchResults);
        }
        if (isCreativeMode(normalizedType) || isCreativeIntent(trimmedContent)) {
            return buildCreativePrompt(trimmedContent, webSearchAttempted, searchResults);
        }
        if (isGeneralSummaryMode(normalizedType) && isSummaryIntent(trimmedContent)) {
            return buildSummaryPrompt(trimmedContent, webSearchAttempted, searchResults);
        }
        return buildFreeChatPrompt(trimmedContent, webSearchAttempted, searchResults);
    }

    public static String buildAskPrompt(String documentContent, String generatedResult, String question) {
        return buildAskPrompt(documentContent, generatedResult, question, false, List.of());
    }

    public static String buildAskPrompt(String documentContent, String generatedResult, String question,
                                        boolean webSearchAttempted,
                                        List<WebSearchResultVO> searchResults) {
        String trimmedQuestion = question == null ? "" : question.trim();
        return """
                你是文枢 NexusDoc 的 AI 文档与知识助手。

                请像正常 GPT 对话一样，直接回答用户追问。当前文档和已生成结果只是上下文，
                不要机械套用“文档摘要、核心要点、行动清单”等固定模板。

                要求：
                1. 用户问什么，就直接回答什么。
                2. 如果问题需要基于当前文档，请优先使用当前文档信息。
                3. 如果开启联网搜索，可以自然融入搜索结果，并在末尾列出“参考来源”。
                4. 如果资料不足，请说明不确定，不要编造事实。
                5. 回答要自然、清楚、具体。

                当前文档原文：
                %s

                已生成内容：
                %s

                用户问题：
                %s
                %s
                """.formatted(blankToEmpty(documentContent), blankToEmpty(generatedResult),
                trimmedQuestion, buildNaturalSearchContext(webSearchAttempted, searchResults));
    }

    private static String buildFreeChatPrompt(String content,
                                              boolean webSearchAttempted,
                                              List<WebSearchResultVO> searchResults) {
        return """
                你是文枢 NexusDoc 的 AI 文档与知识助手。

                你的目标是直接、准确、有用地回答用户问题，而不是机械套用固定模板。

                请遵守以下规则：
                1. 如果用户是在提问，请直接回答问题。
                2. 如果用户是在要求创作，请直接创作内容。
                3. 如果用户是在要求改写，请直接给出改写结果。
                4. 不要把短问题强行分析成“文档摘要、核心要点、行动清单”。
                5. 不要重复输出“原文信息、网络补充、合理推断”这类机械标签。
                6. 如果使用了网络搜索资料，可以自然地融入回答，并在末尾列出参考来源。
                7. 不要编造事实；如果不确定，请明确说明不确定。
                8. 回答要自然、清楚、具体，像正常 GPT 对话一样。

                用户输入：
                %s
                %s
                """.formatted(content, buildNaturalSearchContext(webSearchAttempted, searchResults));
    }

    private static String buildCreativePrompt(String content,
                                              boolean webSearchAttempted,
                                              List<WebSearchResultVO> searchResults) {
        return """
                你是一个擅长中文创作的 AI 助手。请根据用户要求直接生成作品，
                而不是分析用户的请求。

                创作要求：
                1. 直接进入正文，不要先分析“用户想要什么”。
                2. 允许合理想象和文学化表达。
                3. 如果用户没有给出详细设定，可以主动补充背景、冲突和情节。
                4. 故事要有开端、发展、冲突和结尾。
                5. 人物行为要符合设定。
                6. 不要输出“原文未明确提到”这种文档分析式回答。
                7. 不要写成任务清单。
                8. 如果涉及已有角色或 IP，创作内容要标明这是“二创故事”，不要声称是原作剧情。
                9. 内容要适合课程设计展示，避免低俗、暴力、恶意内容。

                用户创作需求：
                %s
                %s
                """.formatted(content, buildNaturalSearchContext(webSearchAttempted, searchResults));
    }

    private static String buildSummaryPrompt(String content,
                                             boolean webSearchAttempted,
                                             List<WebSearchResultVO> searchResults) {
        return """
                你是文枢 NexusDoc 的文档整理助手。请根据用户提供的文档内容，
                生成结构化文档总结。

                要求：
                1. 只基于用户提供的文档内容整理。
                2. 不要编造原文没有的信息。
                3. 如果原文没有明确提到，请写“原文未明确提到”。
                4. 输出要简洁、清晰、有条理。
                5. 不要进行无关发挥。
                6. 不要把用户的创作请求当作文档总结。

                请按照以下格式输出：

                【文档摘要】
                【核心要点】
                【结构化提纲】
                【关键信息】
                【行动清单】
                【风险提醒】
                【术语解释】
                【推荐追问】

                文档内容：
                %s
                %s
                """.formatted(content, buildDocumentSearchContext(webSearchAttempted, searchResults));
    }

    private static String buildStructuredDocumentPrompt(String docType, String content,
                                                        boolean webSearchAttempted,
                                                        List<WebSearchResultVO> searchResults) {
        String format = switch (docType) {
            case "会议纪要" -> """
                    【会议主题】
                    【会议背景】
                    【讨论要点】
                    【决策结果】
                    【待办事项】
                    【负责人】
                    【截止时间】
                    【风险与待确认事项】
                    【推荐追问】
                    """;
            case "工作任务" -> """
                    【任务目标】
                    【核心要求】
                    【具体待办】
                    【优先级建议】
                    【需要确认的问题】
                    【风险提醒】
                    【下一步行动建议】
                    【推荐追问】
                    """;
            case "学习资料" -> """
                    【内容摘要】
                    【核心知识点】
                    【重点难点】
                    【复习提纲】
                    【练习题】
                    【考前速记版】
                    【推荐追问】
                    """;
            case "政策公告" -> """
                    【公告摘要】
                    【政策背景】
                    【适用对象】
                    【核心要求】
                    【重要时间】
                    【影响分析】
                    【用户需要注意的事项】
                    【推荐追问】
                    """;
            case "合同初读" -> """
                    本功能只用于文本理解和信息提取，不构成法律意见。
                    【合同主体】
                    【合同目的】
                    【关键时间】
                    【金额与付款】
                    【双方权利义务】
                    【违约与责任】
                    【需要重点确认的条款】
                    【建议向专业人士确认的问题】
                    【推荐追问】
                    """;
            case "提取重点" -> """
                    【核心重点】
                    【关键事实】
                    【重要结论】
                    【可执行事项】
                    【需要确认的问题】
                    """;
            case "正式改写" -> """
                    【正式改写版本】
                    【结构优化说明】
                    【表达注意事项】
                    """;
            default -> """
                    【内容摘要】
                    【核心信息】
                    【结构化整理】
                    【推荐追问】
                    """;
        };

        return """
                你是文枢 NexusDoc 的文档整理助手。请按用户选择的文档处理模式整理内容。

                要求：
                1. 只在当前模式需要时使用结构化栏目。
                2. 不要把创作请求当作文档总结。
                3. 不要编造原文没有的信息。
                4. 如果原文没有明确提到，请写“原文未明确提到”。

                文档类型：%s
                输出格式：
                %s

                用户内容：
                %s
                %s
                """.formatted(docType, format, content,
                buildDocumentSearchContext(webSearchAttempted, searchResults));
    }

    private static String buildMindMapPrompt(String content,
                                             boolean webSearchAttempted,
                                             List<WebSearchResultVO> searchResults) {
        return """
                请根据用户内容生成思维导图结构数据。

                要求：
                1. 只输出 JSON。
                2. 不要输出 Markdown 代码块。
                3. 不要输出解释。
                4. JSON 必须可以被前端直接解析。
                5. 节点 label 要短，适合显示在文本框中。
                6. 节点层级不要超过 4 层。
                7. 如果启用了联网搜索，可以把网络资料作为补充节点，并用 sourceType 标记。

                JSON 格式：

                {
                  "title": "中心主题",
                  "nodes": [
                    {
                      "id": "1",
                      "label": "一级节点",
                      "level": 1,
                      "parentId": null,
                      "sourceType": "original",
                      "children": [
                        {
                          "id": "1-1",
                          "label": "二级节点",
                          "level": 2,
                          "parentId": "1",
                          "sourceType": "original",
                          "children": []
                        }
                      ]
                    }
                  ],
                  "references": []
                }

                用户内容：
                %s
                %s
                """.formatted(content, buildMindMapSearchContext(webSearchAttempted, searchResults));
    }

    private static String buildTrendPrompt(String content,
                                           boolean webSearchAttempted,
                                           List<WebSearchResultVO> searchResults) {
        return """
                你是一个文档洞察分析助手。请根据用户提供的内容，分析其中可能存在的
                隐藏问题、风险和趋势。

                要求：
                1. 已明确提到的信息必须来自原文。
                2. 趋势判断可以合理推测，但必须说明依据。
                3. 不要把推测说成事实。
                4. 如果依据不足，请写“依据不足，无法判断”。
                5. 如果启用了联网搜索，可以结合搜索结果，但要说明来源。

                输出格式：

                【核心结论】
                【已明确的信息】
                【隐藏问题】
                【潜在风险】
                【趋势判断】
                【依据不足的部分】
                【建议追问】

                用户内容：
                %s
                %s
                """.formatted(content, buildDocumentSearchContext(webSearchAttempted, searchResults));
    }

    private static String buildNaturalSearchContext(boolean webSearchAttempted,
                                                    List<WebSearchResultVO> searchResults) {
        if (!webSearchAttempted) {
            return "\n联网搜索：未启用。";
        }
        if (searchResults == null || searchResults.isEmpty()) {
            return """

                    用户开启了联网搜索，但本次没有检索到有效资料。
                    请直接回答用户问题；如果问题可以创作或推理，就继续完成用户需求。
                    如果问题依赖事实资料，请简洁说明“没有检索到足够有效的网络资料”，不要强行编造事实。
                    """;
        }
        return """

                用户开启了联网搜索功能。
                请结合用户问题和搜索结果，给出自然、有用的回答。

                要求：
                1. 直接回答用户问题，不要机械拆成一堆标签。
                2. 网络搜索结果只作为补充资料，不要照搬搜索摘要。
                3. 如果搜索结果能帮助回答，请自然融入回答中。
                4. 如果搜索结果不足，请明确说明“我没有找到足够可靠的资料”，然后基于已有信息回答。
                5. 如果是创作任务，可以参考搜索结果中的背景设定，但不要声称创作内容是原作事实。
                6. 回答最后用“参考来源”列出用到的来源标题和链接。
                7. 不要输出“原文信息、网络补充、合理推断”这些固定标题，除非用户明确要求区分来源。

                网络搜索结果：
                %s
                """.formatted(formatSearchResults(searchResults));
    }

    private static String buildDocumentSearchContext(boolean webSearchAttempted,
                                                     List<WebSearchResultVO> searchResults) {
        if (!webSearchAttempted) {
            return "\n联网搜索：未启用。";
        }
        if (searchResults == null || searchResults.isEmpty()) {
            return "\n联网搜索：已启用，但没有检索到有效资料。请主要基于用户内容回答。";
        }
        return """

                网络搜索结果仅作为补充资料，不要混入原文事实。若使用搜索结果，请说明来源并在末尾列出参考来源。

                网络搜索结果：
                %s
                """.formatted(formatSearchResults(searchResults));
    }

    private static String buildMindMapSearchContext(boolean webSearchAttempted,
                                                    List<WebSearchResultVO> searchResults) {
        if (!webSearchAttempted) {
            return "\n网络搜索结果：未启用。";
        }
        if (searchResults == null || searchResults.isEmpty()) {
            return "\n网络搜索结果：已尝试联网搜索，但没有检索到有效资料。references 使用空数组。";
        }
        return """

                网络搜索结果：
                %s
                """.formatted(formatSearchResults(searchResults));
    }

    private static String formatSearchResults(List<WebSearchResultVO> searchResults) {
        StringBuilder builder = new StringBuilder();
        for (WebSearchResultVO result : searchResults) {
            builder.append("\n[")
                    .append(result.getIndex())
                    .append("] 标题：")
                    .append(blankToUnknown(result.getTitle()))
                    .append("\n链接：")
                    .append(blankToUnknown(result.getUrl()))
                    .append("\n摘要：")
                    .append(blankToUnknown(result.getSnippet()))
                    .append("\n");
        }
        return builder.toString();
    }

    private static boolean isMindMapMode(String docType, String content) {
        return MIND_MAP.equals(docType)
                || containsAny(content, "生成思维导图", "思维导图", "节点 JSON", "节点JSON");
    }

    private static boolean isTrendMode(String docType, String content) {
        return TREND_ANALYSIS.equals(docType)
                || "趋势分析".equals(docType)
                || containsAny(content, "分析趋势", "趋势分析", "隐藏问题", "风险分析", "潜在风险");
    }

    private static boolean isCreativeMode(String docType) {
        return "内容创作".equals(docType) || "小说设定".equals(docType);
    }

    private static boolean isGeneralSummaryMode(String docType) {
        return GENERAL_SUMMARY.equals(docType);
    }

    private static boolean isStructuredDocumentMode(String docType) {
        return switch (docType) {
            case "会议纪要", "工作任务", "学习资料", "政策公告", "合同初读", "提取重点", "正式改写" -> true;
            default -> false;
        };
    }

    private static boolean isCreativeIntent(String content) {
        return containsAny(content,
                "请写", "编写", "创作", "生成故事", "写小说", "写文案", "续写", "写一段", "帮我写",
                "设定", "世界观", "人物", "剧情", "大纲", "文案", "标题", "脚本");
    }

    private static boolean isSummaryIntent(String content) {
        return containsAny(content, "总结", "概括", "提炼", "提取重点", "整理这份文档", "文档总结")
                && !isCreativeIntent(content)
                && !isQuestionIntent(content)
                && !isRewriteIntent(content);
    }

    private static boolean isQuestionIntent(String content) {
        return containsAny(content, "是什么", "为什么", "怎么做", "如何", "请问", "解释一下",
                "举例", "帮我理解", "这是什么意思");
    }

    private static boolean isRewriteIntent(String content) {
        return containsAny(content, "润色", "优化", "改成", "正式一点", "口语化一点",
                "精简", "扩写", "降重", "翻译");
    }

    private static boolean containsAny(String content, String... keywords) {
        if (content == null || content.isBlank()) {
            return false;
        }
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeDocType(String docType) {
        if (docType == null || docType.isBlank()) {
            return FREE_CHAT;
        }
        String trimmed = docType.trim();
        return switch (trimmed) {
            case "自由对话", "智能回答", "普通对话", "GPT对话", "FREE_CHAT" -> FREE_CHAT;
            case "趋势分析" -> TREND_ANALYSIS;
            default -> trimmed;
        };
    }

    private static String blankToUnknown(String value) {
        return value == null || value.isBlank() ? "未提供" : value;
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value;
    }
}
