package com.utm.rugbyplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WorkoutPlanResponse — returned by all /api/workout endpoints.
 *
 * Contains both the input parameters (for display in UI)
 * and the AI-generated plan text.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutPlanResponse {

    private String  id;
    private String  userId;
    private String  planName;

    // ── Input parameters ─────────────────────────────────────
    private String  rugbyPosition;
    private String  goal;
    private String  trainingLevel;
    private Integer weight;
    private Integer height;
    private Integer age;
    private String  injuryNotes;
    private Integer sessionsPerWeek;
    private String  trainingPhase;
    private String  availableEquipment;
    private String  focusArea;

    // ── AI output ─────────────────────────────────────────────
    /** Full markdown text returned by Ollama llama3.2 */
    private String  generatedPlan;

    /** The AI's original output, present only once the plan has been edited. */
    private String  originalPlan;

    // ── Trainer audit fields ──────────────────────────────────
    private String trainerNote;
    private String lastEditedBy;

    // ── UC005 manage fields ───────────────────────────────────
    /**
     * @JsonProperty forces Jackson to serialise/deserialise as "isActive".
     * Without it, Lombok generates isActive() getter and Jackson strips the
     * "is" prefix, producing JSON key "active" instead of "isActive".
     */
    @JsonProperty("isActive")
    private boolean      isActive;
    private List<String> completedItems;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
