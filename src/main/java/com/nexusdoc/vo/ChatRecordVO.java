package com.nexusdoc.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatRecordVO {

    private Long id;

    private String question;

    private String answer;

    private LocalDateTime createTime;
}
