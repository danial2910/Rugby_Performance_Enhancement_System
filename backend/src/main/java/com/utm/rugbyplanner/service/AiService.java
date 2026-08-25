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

    @Value("${groq.model:openai/gpt-oss-120b}")
    private String groqModel;

    // Default completion length cap — see the comment in generate() for why this
    // exists and why it must stay under the model's tokens-per-minute (TPM) budget.
    // Plan generation (meal/workout) uses this full budget; the chatbot passes a
    // much smaller one via the generate(prompt, maxTokens) overload.
    @Value("${groq.max-completion-tokens:7000}")
    private Integer maxCompletionTokens;

    // openai/gpt-oss-* are reasoning models: hidden reasoning tokens are billed as
    // completion tokens and eat into the same cap the visible answer needs. "low"
    // keeps that overhead to a few hundred tokens instead of ~900, which matters a
    // lot when the whole per-minute allowance is only 8000.
    @Value("${groq.reasoning-effort:low}")
    private String reasoningEffort;

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
        return generate(prompt, maxCompletionTokens);
    }

    /**
     * Send a prompt to Groq with an explicit completion-length cap.
     *
     * <p>Short-answer callers (the chatbot) should pass a small cap. Groq reserves
     * prompt + maxTokens against the per-minute token allowance up front, so a
     * chatbot reply asking for the full plan-sized budget would consume almost the
     * entire minute and starve plan generation.
     *
     * @param prompt     The full prompt to send to the model.
     * @param maxTokens  Upper bound on completion tokens (reasoning included).
     */
    public String generate(String prompt, int maxTokens) {
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
        // Summary table is ever written).
        //
        // The cap must also stay UNDER the model's tokens-per-minute (TPM) limit:
        // Groq budgets prompt tokens + max_completion_tokens up front and rejects
        // the whole request with 413 if the sum exceeds the TPM allowance
        // (8000 for openai/gpt-oss-120b on the free on_demand tier). Hence 7000,
        // which leaves room for the prompt itself. Override with
        // groq.max-completion-tokens if you move to a tier with a higher limit.
        GroqRequest request = new GroqRequest(
                groqModel, List.of(message), maxTokens, 0.4, reasoningEffort);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);
        HttpEntity<GroqRequest> entity = new HttpEntity<>(request, headers);

        log.debug("Calling Groq at {} with model={}, maxCompletionTokens={}",
                url, groqModel, maxTokens);

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
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS
                    || e.getStatusCode() == HttpStatus.PAYLOAD_TOO_LARGE) {
                throw new RuntimeException(
                        "AI engine is busy — the per-minute token limit was reached. " +
                        "Please wait a minute and try again.", e);
            }
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
        // Reasoning models only. Groq ignores this for non-reasoning models, so it
        // is safe to always send.
        @com.fasterxml.jackson.annotation.JsonProperty("reasoning_effort")
        private String reasoningEffort;
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
