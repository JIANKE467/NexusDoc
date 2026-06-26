package com.nexusdoc.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DeviceDataExportVO {

    private String version;

    private LocalDateTime exportedAt;

    private Integer documentCount;

    private List<DocumentBackupVO> documents;

    @Data
    @Builder
    public static class DocumentBackupVO {

        private String title;

        private String docType;

        private String tag;

        private String content;

        private String resultText;

        private LocalDateTime createTime;

        private List<ChatBackupVO> chatRecords;
    }

    @Data
    @Builder
    public static class ChatBackupVO {

        private String question;

        private String answer;

        private LocalDateTime createTime;
    }
}
