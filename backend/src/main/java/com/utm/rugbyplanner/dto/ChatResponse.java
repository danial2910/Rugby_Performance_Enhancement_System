package com.utm.rugbyplanner.dto;

import lombok.*;

/**
 * UC011: Chat with AI Chatbot — response payload for POST /api/chatbot/message
 *
 * Returns both persisted turns (the user's message as stored, and the
 * assistant's reply) so the Vue chat UI can append them with their
 * real IDs/timestamps without re-fetching the whole history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private ChatMessageResponse userMessage;
    private ChatMessageResponse assistantMessage;
}
