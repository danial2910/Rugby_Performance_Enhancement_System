package com.utm.rugbyplanner.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

/**
 * AiService — AI text-generation client.
 *
 * Every AI feature (workout plans, meal plans, chatbot) calls
 * {@link #generate(String)} on this single class.
 *
 * Calls Groq's free, hosted, OpenAI-compatible chat-completions API:
 *   POST {groq.api.url}/chat/completions
 * Requires only a free API key from https://console.groq.com — no local
 * model server needed, which is what makes this work on lightweight
 * free-tier hosts.
 *
 * (This service used to call a local Ollama server for development. The
 * project has since fully switched to Groq everywhere, local and deployed,
 * so that local dev and production behave identically.)
 */
@Slf4j
@Service
public class AiService {

    private final RestTemplate restTemplate;

    @Value("${groq.api.url:https://api.groq.com/openai/v1}")
    private String groqApiUrl;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    public AiService(
            @Value("${groq.timeout-seconds:60}") long timeoutSeconds,
            RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    /**
     * Send a prompt to Groq and return the generated text.
     *
     * @param prompt  The full prompt to send to the model.
     * @return        The model's text response.
     * @throws RuntimeException if Groq is not reachable, not configured, or returns an error.
     */
    public String generate(String prompt) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            throw new RuntimeException(
                    "AI engine is not configured. GROQ_API_KEY is missing — " +
                    "set it as an environment variable.");
        }

        String url = groqApiUrl + "/chat/completions";

        GroqMessage message = new GroqMessage("user", prompt);
        // IMPORTANT — Groq's default max_completion_tokens (when omitted) is far
        // too small for a full 7-day meal/workout plan that also has to show its
        // per-day addition work before the closing summary table. Without an
        // explicit, generous cap, the API silently truncates the response mid-plan
        // (e.g. cutting off partway through Day 6, before the Weekly Nutrition
        // Summary table is ever written). 8000 tokens comfortably covers the
        // largest plans this app generates while staying within the model's limits.
        GroqRequest request = new GroqRequest(groqModel, List.of(message), 8000, 0.4);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);
        HttpEntity<GroqRequest> entity = new HttpEntity<>(request, headers);

        log.debug("Calling Groq at {} with model={}", url, groqModel);

        try {
            ResponseEntity<GroqResponse> response =
                    restTemplate.postForEntity(url, entity, GroqResponse.class);

            if (response.getStatusCode() == HttpStatus.OK
                    && response.getBody() != null
                    && response.getBody().getChoices() != null
                    && !response.getBody().getChoices().isEmpty()) {

                String result = response.getBody().getChoices().get(0)
                        .getMessage().getContent().trim();
                log.debug("Groq response length: {} chars", result.length());
                return result;
            }

            throw new RuntimeException("Groq returned an empty response.");

        } catch (HttpClientErrorException e) {
            log.error("Groq API error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(
                    "AI engine returned an error (" + e.getStatusCode() + "). " +
                    "Check that GROQ_API_KEY is valid and not rate-limited.", e);
        } catch (ResourceAccessException e) {
            log.error("Cannot reach Groq at {}: {}", url, e.getMessage());
            throw new RuntimeException(
                    "AI engine is not available right now. Please try again shortly.", e);
        }
    }

    // ── Internal DTOs — Groq (OpenAI-compatible chat completions) ──────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class GroqMessage {
        private String role;
        private String content;
    }

    @Data
    @AllArgsConstructor
    private static class GroqRequest {
        private String model;
        private List<GroqMessage> messages;
        // Explicit completion length cap — see comment at the call site in
        // generate() for why this must not be left to Groq's default.
        @com.fasterxml.jackson.annotation.JsonProperty("max_completion_tokens")
        private Integer maxCompletionTokens;
        // Lower temperature → more deterministic, more reliable arithmetic when
        // the model is asked to show its addition work for daily macro totals.
        private Double temperature;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GroqChoice {
        private GroqMessage message;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GroqResponse {
        private List<GroqChoice> choices;
    }
}
