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
                2. 输出要清晰、简洁、适合普通用户理解。
                3. 严格使用以下栏目格式。

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
