package com.nexusdoc.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum DocumentTypeEnum {
    GENERAL_SUMMARY("通用总结"),
    MEETING_MINUTES("会议纪要"),
    WORK_TASK("工作任务"),
    STUDY_MATERIAL("学习资料"),
    POLICY_NOTICE("政策公告"),
    CONTRACT_READING("合同初读"),
    CONTENT_CREATION("内容创作");

    private final String displayName;

    DocumentTypeEnum(String displayName) {
        this.displayName = displayName;
    }

    public static boolean supports(String displayName) {
        return Arrays.stream(values())
                .anyMatch(type -> type.displayName.equals(displayName));
    }
}
