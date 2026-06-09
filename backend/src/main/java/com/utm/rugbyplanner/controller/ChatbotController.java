package com.utm.rugbyplanner.controller;

import com.utm.rugbyplanner.dto.ApiResponse;
import com.utm.rugbyplanner.dto.ChatMessageResponse;
import com.utm.rugbyplanner.dto.ChatRequest;
import com.utm.rugbyplanner.dto.ChatResponse;
import com.utm.rugbyplanner.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ChatbotController — UC011: Chat with AI Chatbot
 *
 * All endpoints require a valid JWT Bearer token (any authenticated role —
 * the SRS Use Case Specification names "Client" as the actor, and both
 * athletes and trainers benefit from quick fitness/nutrition/system Q&A).
 *
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │  Method  │  URL                  │  Purpose                          │
 * ├──────────┼───────────────────────┼───────────────────────────────────┤
 * │  GET     │  /api/chatbot/history │  Load saved conversation          │
 * │  POST    │  /api/chatbot/message │  Send a message, get AI reply     │
 * │  DELETE  │  /api/chatbot/history │  Clear the conversation           │
 * └──────────────────────────────────────────────────────────────────────┘
 */
@Slf4j
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * GET /api/chatbot/history
     *
     * UC011 Normal Flow Step 2: "System displays chat interface" — this
     * endpoint feeds the interface with any previously saved conversation
     * so the user can pick up where they left off.
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<ChatMessageResponse> history = chatbotService.getHistory(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Chat history loaded.", history));
    }

    /**
     * POST /api/chatbot/message
     *
     * UC011 Normal Flow Steps 3-7: persists the user's message, builds a
     * personalised prompt (profile + recent history), calls the Ollama
     * llama3.2 model, persists and returns the assistant's reply.
     *
     * 200 OK → { userMessage, assistantMessage }
     * 400    → @Valid failure (blank / too-long message)
     *
     * Note: unlike the plan-generation endpoints, EF1 (AI engine down) is
     * handled inside the service so the chat keeps working with a graceful
     * fallback message rather than surfacing a hard error to the user.
     */
    @PostMapping("/message")
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChatRequest request) {

        log.debug("UC011 Chat — POST /api/chatbot/message — user: {}", userDetails.getUsername());

        ChatResponse response = chatbotService.sendMessage(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Message sent.", response));
    }

    /**
     * DELETE /api/chatbot/history
     *
     * Lets the user start a fresh conversation (clears persisted turns).
     */
    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse<Void>> clearHistory(
            @AuthenticationPrincipal UserDetails userDetails) {

        chatbotService.clearHistory(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Chat history cleared.", null));
    }
}
