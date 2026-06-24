package com.nexusdoc.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatAnswerVO {

    private Long chatRecordId;

    private Long documentId;

    private String question;

    private String answer;

    private LocalDateTime createTime;
}
