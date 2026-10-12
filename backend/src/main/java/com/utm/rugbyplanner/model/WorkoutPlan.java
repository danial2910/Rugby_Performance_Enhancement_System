package com.utm.rugbyplanner.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * WorkoutPlan — MongoDB collection: "workout_plans"
 *
 * UC004: Create Workout Plan
 * UC005: Manage Workout Plan
 *
 * Each document stores one AI-generated weekly workout plan
 * linked to a User via userId. The raw AI response is stored
 * in generatedPlan (formatted markdown text from Ollama).
 *
 * Inputs used to generate the plan:
 *   - rugbyPosition    → determines training focus (strength / speed / agility)
 *   - goal             → STRENGTH | ENDURANCE | LEAN | POWER
 *   - trainingLevel    → BEGINNER | INTERMEDIATE | ADVANCED
 *   - weight, height, age → physical stats for volume/intensity tuning
 *   - injuryNotes      → AI avoids exercises that aggravate existing injuries
 *   - sessionsPerWeek  → number of training days (3–6)
 *
 * UC005 manage fields:
 *   - isActive         → true if user designated this as their current plan
 *   - completedItems   → list of day/session keys user has checked off
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "workout_plans")
public class WorkoutPlan {

    @Id
    private String id;

    @Indexed
    private String userId;          // FK → User._id

    /** User-defined name for this plan (e.g. "Pre-Season Strength Block") */
    private String planName;

    // ── Input parameters sent to Ollama ──────────────────────────────────
    private String  rugbyPosition;   // "Prop", "Flanker", "Fullback", etc.
    private String  goal;            // "STRENGTH" | "ENDURANCE" | "LEAN" | "POWER"
    private String  trainingLevel;   // "BEGINNER" | "INTERMEDIATE" | "ADVANCED"
    private Integer weight;          // kg
    private Integer height;          // cm
    private Integer age;             // years
    private String  injuryNotes;       // free text, nullable
    private Integer sessionsPerWeek;   // 2 – 7
    private String  trainingPhase;     // "PRE_SEASON" | "IN_SEASON" | "OFF_SEASON" | "POST_SEASON"
    private String  availableEquipment;// "GYM" | "HOME" | "FIELD" | "MINIMAL"
    private String  focusArea;         // optional free text

    /** Full plan text (markdown). Overwritten when the user edits. */
    private String generatedPlan;

    /**
     * The AI's untouched output, captured the first time the user edits this
     * plan. Lets the athlete restore the original after a bad edit without
     * paying for another 20-60s generation. Null until the first edit.
     */
    private String originalPlan;

    // ── Trainer edit audit fields ─────────────────────────────────────────────
    /** Note the trainer must provide when modifying this plan */
    private String trainerNote;

    /** Display name of the trainer who last edited this plan */
    private String lastEditedBy;

    // ── UC005 manage fields ───────────────────────────────────────────────────
    /** True if the user has set this as their currently active plan */
    @Builder.Default
    private boolean isActive = false;

    /**
     * Keys of sessions/days the user has checked off as completed.
     * Stored as strings like "Day 1", "Day 2", etc.
     */
    @Builder.Default
    private List<String> completedItems = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
