package com.utm.rugbyplanner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * UC011: Chat with AI Chatbot — request body for POST /api/chatbot/message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "Please type a message before sending.")
    @Size(max = 2000, message = "Message must be at most 2000 characters.")
    private String message;
}
