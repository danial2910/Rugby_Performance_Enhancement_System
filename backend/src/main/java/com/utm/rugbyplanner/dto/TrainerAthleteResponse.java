package com.utm.rugbyplanner.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * TrainerAthleteResponse — lightweight athlete record for the trainer's athlete list.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerAthleteResponse {

    private String id;
    private String username;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String matrixNumber;
    private String rugbyPosition;   // from profile if available, else null
    private long   workoutPlanCount;
    private long   mealPlanCount;
    private int    workoutProgress;   // 0–100 % active workout plan completion
    private int    mealProgress;      // 0–100 % active meal plan completion
    private LocalDateTime joinedAt;
}
