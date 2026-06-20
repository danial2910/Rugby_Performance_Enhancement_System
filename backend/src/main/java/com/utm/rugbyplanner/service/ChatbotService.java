package com.utm.rugbyplanner.service;

import com.utm.rugbyplanner.dto.ChatMessageResponse;
import com.utm.rugbyplanner.dto.ChatRequest;
import com.utm.rugbyplanner.dto.ChatResponse;
import com.utm.rugbyplanner.model.Athlete;
import com.utm.rugbyplanner.model.ChatMessage;
import com.utm.rugbyplanner.model.User;
import com.utm.rugbyplanner.repository.AthleteRepository;
import com.utm.rugbyplanner.repository.ChatMessageRepository;
import com.utm.rugbyplanner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ChatbotService — UC011: Chat with AI Chatbot
 *
 * Normal Flow (SRS Table 3.3 / Use Case Specification UC011):
 *   1. User accesses the AI chatbot through the dashboard / dedicated chat interface
 *   2. System displays the chat interface with a prompt to ask questions
 *   3. User types a question or request related to fitness, nutrition, or system usage
 *   4. System analyses the user input (delegated to Groq's hosted LLM)
 *   5. AI considers the user's profile, goals, and history for personalisation
 *   6. System "searches the knowledge base" — implemented as a system prompt
 *      describing the platform so the model can answer system-usage questions
 *   7. AI generates a personalised response based on user context
 *
 * Alternative / Exception flows:
 *   AF1 Unclear query        → handled by instructing the model to ask for
 *                              clarification when the question is ambiguous
 *   AF2 No knowledge match   → the model is instructed to give general safe
 *                              advice and to point to a professional / emergency
 *                              services when the question is outside its scope
 *   EF1 System error/down    → caught here; a friendly fallback message is
 *                              persisted and returned instead of failing silently
 *                              (matches STD TC011_03 expected result)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    /** How many previous turns to replay back to the model as context */
    private static final int HISTORY_CONTEXT_SIZE = 10;

    private static final String UNAVAILABLE_MESSAGE =
            "Chatbot unavailable. Please try again later or contact support.";

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository        userRepository;
    private final AthleteRepository     athleteRepository;
    private final AiService             aiService;

    // ── UC011 Step 2: Load chat history when the view mounts ─────────────────

    public List<ChatMessageResponse> getHistory(String username) {
        User user = findUser(username);
        return chatMessageRepository.findByUserIdOrderByCreatedAtAsc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── UC011 Steps 3-7: Send a message and get a personalised reply ─────────

    public ChatResponse sendMessage(String username, ChatRequest request) {
        User user = findUser(username);
        String question = request.getMessage().trim();

        log.info("UC011 Chat — user: {}, message length: {}", username, question.length());

        // Persist the user's turn first so it survives even if the AI call fails
        ChatMessage userTurn = chatMessageRepository.save(
                ChatMessage.builder()
                        .userId(user.getId())
                        .role("USER")
                        .content(question)
                        .build()
        );

        // Pull recent turns (oldest → newest) for conversational context
        List<ChatMessage> recentHistory = chatMessageRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, HISTORY_CONTEXT_SIZE + 1))
                .stream()
                .filter(m -> !m.getId().equals(userTurn.getId()))
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
                .collect(Collectors.toList());

        String prompt = buildPrompt(user, question, recentHistory);

        String replyText;
        try {
            replyText = aiService.generate(prompt);
            if (replyText == null || replyText.isBlank()) {
                replyText = "I couldn't come up with an answer for that — could you try rephrasing your question?";
            }
        } catch (RuntimeException e) {
            // EF1: System error or downtime — respond gracefully instead of a 500
            log.warn("UC011 EF1 — chatbot unavailable for user {}: {}", username, e.getMessage());
            replyText = UNAVAILABLE_MESSAGE;
        }

        ChatMessage assistantTurn = chatMessageRepository.save(
                ChatMessage.builder()
                        .userId(user.getId())
                        .role("ASSISTANT")
                        .content(replyText)
                        .build()
        );

        return ChatResponse.builder()
                .userMessage(toResponse(userTurn))
                .assistantMessage(toResponse(assistantTurn))
                .build();
    }

    // ── Clear the conversation ────────────────────────────────────────────────

    public void clearHistory(String username) {
        User user = findUser(username);
        chatMessageRepository.deleteByUserId(user.getId());
        log.info("UC011 Chat history cleared — user: {}", username);
    }

    // ── Prompt builder ────────────────────────────────────────────────────────

    /**
     * Builds a system + context + conversation prompt for Groq.
     *
     * Personalisation (Normal Flow Step 5): when the caller is an athlete with
     * a profile on file, their goal, position, training level and injury notes
     * are folded into the system prompt so answers are tailored to them.
     *
     * "Knowledge base" (Normal Flow Step 6) is implemented as a description of
     * what the platform can do, so the assistant can answer "how do I…" style
     * system-navigation questions (per the SRS chatbot module description).
     */
    private String buildPrompt(User user, String question, List<ChatMessage> history) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
You are the friendly AI wellness assistant built into the Rugby Performance Enhancement \
System (UTM Pirates) — a platform for university rugby athletes and their trainers.

WHAT THE PLATFORM CAN DO (use this to answer "how do I…" questions):
- Athletes can generate and manage AI-powered weekly workout plans and 7-day meal plans.
- Athletes can book, edit, and cancel appointments with trainers; trainers approve or reject them.
- Users can update their profile (physical stats, goals, dietary preferences, injuries, etc.).
- Athletes receive email notifications when a trainer approves/rejects an appointment.

HOW TO RESPOND:
- Answer questions about rugby fitness, nutrition, recovery, and how to use this platform.
- Keep replies concise, encouraging, and easy to read — use short paragraphs or bullet points.
- If the question is vague or ambiguous, briefly ask the user to clarify what they mean \
instead of guessing (for example: "Could you clarify your question? Are you asking about \
specific foods or a full meal plan?").
- If the question is outside fitness/nutrition/platform-support (e.g. medical emergencies, \
injuries that sound serious, or topics you have no reliable information on), give brief \
general guidance and recommend the user consult a qualified professional or, if urgent, \
contact emergency services. Do not invent medical advice.
- Never claim to be a doctor, dietitian, or medical professional.
""");

        sb.append("\nUSER PROFILE:\n");
        sb.append("- Name: ").append(user.getFullName() != null ? user.getFullName() : user.getUsername()).append('\n');
        sb.append("- Role: ").append(user.getUserRole()).append('\n');

        if (user.getUserRole() == User.UserRole.ATHLETE) {
            Athlete athlete = athleteRepository.findByUserId(user.getId()).orElse(null);
            if (athlete != null) {
                appendIfPresent(sb, "Rugby position", athlete.getRugbyPosition());
                appendIfPresent(sb, "Goal", athlete.getGoal());
                appendIfPresent(sb, "Training level", athlete.getTrainingLevel());
                appendIfPresent(sb, "Activity level", athlete.getActivityLevel());
                appendIfPresent(sb, "Dietary restrictions", athlete.getDietaryRestrictions());
                appendIfPresent(sb, "Injury notes", athlete.getInjuryNotes());
                if (athlete.getWeight() != null && athlete.getHeight() != null && athlete.getAge() != null) {
                    sb.append("- Physical stats: ")
                            .append(athlete.getWeight()).append(" kg, ")
                            .append(athlete.getHeight()).append(" cm, ")
                            .append(athlete.getAge()).append(" years old\n");
                }
            }
        }

        if (!history.isEmpty()) {
            sb.append("\nRECENT CONVERSATION (oldest first):\n");
            for (ChatMessage m : history) {
                String speaker = "USER".equals(m.getRole()) ? "User" : "Assistant";
                sb.append(speaker).append(": ").append(m.getContent()).append('\n');
            }
        }

        sb.append("\nNow respond to the user's latest message.\n");
        sb.append("User: ").append(question).append('\n');
        sb.append("Assistant:");

        return sb.toString();
    }

    private void appendIfPresent(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append("- ").append(label).append(": ").append(value).append('\n');
        }
    }

    // ── Mapper / helpers ──────────────────────────────────────────────────────

    private ChatMessageResponse toResponse(ChatMessage m) {
        return ChatMessageResponse.builder()
                .id(m.getId())
                .role(m.getRole())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
