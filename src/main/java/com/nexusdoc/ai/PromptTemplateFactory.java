package com.nexusdoc.ai;

import com.nexusdoc.enums.DocumentTypeEnum;

public final class PromptTemplateFactory {

    private PromptTemplateFactory() {
    }

    public static String buildDocumentPrompt(String docType, String content) {
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
            case "内容创作" -> """
                    【素材摘要】
                    【可用观点】
                    【适合的内容选题】
                    【文章/视频大纲】
                    【标题建议】
                    【可引用金句】
                    【需要补充的信息】
                    【推荐追问】
                    """;
            case "思维导图" -> """
                    请根据用户提供的内容生成思维导图结构数据。
                    只输出 JSON，不要输出解释、寒暄或 Markdown 代码块。

                    JSON 格式必须如下：
                    {
                      "title": "中心主题",
                      "nodes": [
                        {
                          "id": "1",
                          "label": "一级节点",
                          "level": 1,
                          "parentId": null,
                          "children": [
                            {
                              "id": "1-1",
                              "label": "二级节点",
                              "level": 2,
                              "parentId": "1",
                              "children": []
                            }
                          ]
                        }
                      ]
                    }

                    要求：
                    1. title 是中心主题。
                    2. nodes 是一级节点数组。
                    3. 每个节点必须有 id、label、level、parentId、children。
                    4. label 不要太长，适合显示在文本框中。
                    5. 节点层级建议不超过 4 层。
                    6. 一级节点建议 3 到 6 个。
                    7. 每个一级节点下建议 2 到 5 个子节点。
                    8. 不要输出图片。
                    9. 不要输出 Mermaid。
                    10. 不要输出 Markdown。
                    11. 不要编造原文没有的信息。
                    12. 如果原文信息不足，请生成较少节点，并在节点中体现“原文未明确提到”。
                    """;
            case "小说设定" -> """
                    【故事核心概念】
                    【世界观设定】
                    【时代背景】
                    【主要势力】
                    【主角设定】
                    【重要配角】
                    【人物关系】
                    【核心冲突】
                    【主线剧情】
                    【支线剧情】
                    【章节规划】
                    【伏笔设计】
                    【风格建议】
                    """;
            case "趋势与隐藏问题分析" -> """
                    【文档摘要】
                    【已明确提到的信息】
                    【可能隐藏的问题】
                    【潜在风险】
                    【后续趋势判断】
                    【需要补充的数据】
                    【建议追问的问题】
                    """;
            default -> """
                    【文档摘要】
                    【核心要点】
                    【结构化提纲】
                    【关键信息】
                    【行动清单】
                    【风险提醒】
                    【术语解释】
                    【推荐追问】
                    """;
        };

        return """
                你是文枢 NexusDoc 的文档理解助手。请基于用户提供的原文生成结构化文档工作包。
                要求：
                1. 只基于原文内容输出，不要编造事实。
                2. 如果原文没有明确提到，请写“原文未明确提到”。
                3. 输出要清晰、简洁、适合普通用户理解。
                4. 不要输出无关寒暄。
                5. 严格使用以下栏目格式。
                6. 小说设定模式允许合理创作，但内容必须适合课程项目展示。
                7. 趋势判断必须标明“根据原文推测”，依据不足时写“依据不足，无法判断”。

                文档类型：%s
                输出格式：
                %s

                文档原文：
                %s
                """.formatted(normalizeDocType(docType), format, content);
    }

    public static String buildAskPrompt(String documentContent, String generatedResult, String question) {
        return """
                你是文枢 NexusDoc 的文档追问助手。请严格遵守：
                1. 只基于当前文档内容回答。
                2. 不要编造文档中没有的信息。
                3. 如果原文没有明确提到，回答“原文未明确提到”。
                4. 回答要简洁、清楚，适合普通用户理解。
                5. 不要偏离当前文档主题。

                当前文档原文：
                %s

                已生成的文档工作包：
                %s

                用户问题：
                %s
                """.formatted(documentContent, generatedResult, question);
    }

    private static String normalizeDocType(String docType) {
        return DocumentTypeEnum.supports(docType) ? docType : DocumentTypeEnum.GENERAL_SUMMARY.getDisplayName();
    }
}
