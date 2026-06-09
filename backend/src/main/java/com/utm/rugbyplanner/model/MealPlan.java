package com.utm.rugbyplanner.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MealPlan — MongoDB collection: "meal_plans"
 *
 * UC006: Create Meal Plan
 * UC007: Manage Meal Plan
 *
 * Each document stores one AI-generated 7-day meal plan
 * linked to a User via userId. The raw AI response is stored
 * in generatedPlan (formatted markdown text from Ollama).
 *
 * Inputs used to generate the plan:
 *   - rugbyPosition     → affects caloric demands (props vs backs)
 *   - goals             → one or more of: MUSCLE_GAIN | WEIGHT_LOSS | MAINTAIN | PERFORMANCE
 *   - weight, height, age → used to estimate TDEE
 *   - dietaryPreference → HALAL | VEGETARIAN | VEGAN | NO_RESTRICTION
 *   - allergies         → free text — AI excludes allergen ingredients
 *   - mealsPerDay       → 3 – 6 meals per day
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "meal_plans")
public class MealPlan {

    @Id
    private String id;

    @Indexed
    private String userId;           // FK → User._id

    /** User-defined name for this plan (e.g. "Pre-Season Bulking Week") */
    private String planName;

    // ── Input parameters sent to Ollama ──────────────────────────────────
    private String       rugbyPosition;  // "Prop", "Flanker", "Fullback", etc.
    private List<String> goals;         // e.g. ["MUSCLE_GAIN", "PERFORMANCE"]
    private Integer weight;            // kg
    private Integer height;            // cm
    private Integer age;               // years
    private String  dietaryPreference; // "HALAL" | "VEGETARIAN" | "VEGAN" | "NO_RESTRICTION"
    private String  allergies;         // free text, nullable (e.g. "peanuts, dairy")
    private Integer mealsPerDay;       // 3 – 6
    private String  activityLevel;     // "MODERATE" | "ACTIVE" | "EXTREME"
    private Integer targetWeight;      // kg, nullable
    private String  trainingPhase;     // "PRE_SEASON" | "IN_SEASON" | "OFF_SEASON" | "POST_SEASON"
    private String  mealPrepTime;      // "LOW" | "MEDIUM" | "HIGH"

    /** Full AI-generated 7-day meal plan text (markdown from Ollama llama3.2) */
    private String generatedPlan;

    // ── Trainer edit audit fields ─────────────────────────────────────────────
    /** Note the trainer must provide when modifying this plan */
    private String trainerNote;

    /** Display name of the trainer who last edited this plan */
    private String lastEditedBy;

    // ── UC007 manage fields ───────────────────────────────────────────────────
    /** True if the user has set this as their currently active meal plan */
    @Builder.Default
    private boolean isActive = false;

    /**
     * Keys of meals/days the user has checked off as completed.
     * Stored as strings like "Day 1 – Breakfast", "Day 1 – Lunch", etc.
     */
    @Builder.Default
    private List<String> completedItems = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
