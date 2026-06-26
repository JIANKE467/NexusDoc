package com.nexusdoc.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class DeviceDataImportRequest {

    private List<DocumentBackup> documents = new ArrayList<>();

    @Data
    public static class DocumentBackup {

        private String title;

        private String docType;

        private String tag;

        private String content;

        private String resultText;

        private LocalDateTime createTime;

        private List<ChatBackup> chatRecords = new ArrayList<>();
    }

    @Data
    public static class ChatBackup {

        private String question;

        private String answer;

        private LocalDateTime createTime;
    }
}
