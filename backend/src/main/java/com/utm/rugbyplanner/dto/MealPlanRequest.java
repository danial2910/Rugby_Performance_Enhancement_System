package com.utm.rugbyplanner.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/**
 * MealPlanRequest — body for POST /api/meal/generate
 *
 * All fields feed into the Ollama prompt for UC006: Create Meal Plan.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealPlanRequest {

    /** Optional custom name. Auto-generated if blank. */
    private String planName;

    @NotBlank(message = "Rugby position is required.")
    private String rugbyPosition;

    @NotNull(message = "At least one nutrition goal is required.")
    @Size(min = 1, message = "At least one nutrition goal is required.")
    private List<String> goals;      // e.g. ["MUSCLE_GAIN", "PERFORMANCE"]

    @NotNull(message = "Weight is required.")
    @Min(value = 40, message = "Weight must be at least 40 kg.")
    @Max(value = 200, message = "Weight must be at most 200 kg.")
    private Integer weight;

    @NotNull(message = "Height is required.")
    @Min(value = 140, message = "Height must be at least 140 cm.")
    @Max(value = 220, message = "Height must be at most 220 cm.")
    private Integer height;

    @NotNull(message = "Age is required.")
    @Min(value = 15, message = "Age must be at least 15.")
    @Max(value = 60, message = "Age must be at most 60.")
    private Integer age;

    @NotBlank(message = "Dietary preference is required.")
    private String dietaryPreference; // HALAL | VEGETARIAN | VEGAN | NO_RESTRICTION

    /** Optional — e.g. "peanuts, dairy, gluten". Nullable is fine. */
    private String allergies;

    @NotNull(message = "Meals per day is required.")
    @Min(value = 3, message = "Minimum 3 meals per day.")
    @Max(value = 6, message = "Maximum 6 meals per day.")
    private Integer mealsPerDay;

    /** MODERATE | ACTIVE | EXTREME — used to calculate accurate TDEE */
    @NotBlank(message = "Activity level is required.")
    private String activityLevel;

    /** Target body weight in kg — helps AI calibrate surplus/deficit */
    @Min(value = 40, message = "Target weight must be at least 40 kg.")
    @Max(value = 200, message = "Target weight must be at most 200 kg.")
    private Integer targetWeight;

    /** PRE_SEASON | IN_SEASON | OFF_SEASON | POST_SEASON */
    @NotBlank(message = "Training phase is required.")
    private String trainingPhase;

    /** LOW | MEDIUM | HIGH — how much time athlete has to prepare meals */
    @NotBlank(message = "Meal prep time is required.")
    private String mealPrepTime;
}
