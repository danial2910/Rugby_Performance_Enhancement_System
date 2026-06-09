package com.utm.rugbyplanner.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * UC011: Chat with AI Chatbot — single message returned to the Vue chat UI
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    private String id;

    /** "USER" or "ASSISTANT" */
    private String role;

    private String content;

    private LocalDateTime createdAt;
}
