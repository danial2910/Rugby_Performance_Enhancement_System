package com.utm.rugbyplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MealPlanResponse — returned by all /api/meal endpoints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealPlanResponse {

    private String  id;
    private String  userId;
    private String  planName;

    // ── Input parameters ─────────────────────────────────────
    private String       rugbyPosition;
    private List<String> goals;
    private Integer weight;
    private Integer height;
    private Integer age;
    private String  dietaryPreference;
    private String  allergies;
    private Integer mealsPerDay;
    private String  activityLevel;
    private Integer targetWeight;
    private String  trainingPhase;
    private String  mealPrepTime;

    // ── AI output ─────────────────────────────────────────────
    /** Full 7-day markdown meal plan returned by Ollama llama3.2 */
    private String  generatedPlan;

    // ── Trainer audit fields ──────────────────────────────────
    private String trainerNote;
    private String lastEditedBy;

    // ── UC007 manage fields ───────────────────────────────────
    /** @JsonProperty forces "isActive" JSON key — Lombok @Data getter isActive() would otherwise produce "active". */
    @JsonProperty("isActive")
    private boolean      isActive;
    private List<String> completedItems;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
