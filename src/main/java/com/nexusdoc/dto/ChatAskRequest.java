package com.nexusdoc.dto;

import lombok.Data;

@Data
public class ChatAskRequest {

    private Long userId;

    private Long documentId;

    private String question;
}
