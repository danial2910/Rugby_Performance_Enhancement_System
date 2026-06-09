package com.utm.rugbyplanner.model;

import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Appointment — MongoDB collection: "appointments"
 *
 * UC008: Create Appointment (athlete initiates)
 * UC009: Manage Appointment (athlete views / cancels)
 * UC012: Approve Appointment (trainer approves / rejects)
 *
 * Status lifecycle:
 *   PENDING  → athlete just booked, waiting for trainer
 *   APPROVED → trainer accepted the request
 *   REJECTED → trainer declined; remarks field contains reason
 *   CANCELLED → athlete cancelled before trainer responded
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "appointments")
public class Appointment {

    @Id
    private String id;

    /** User.id of the athlete who booked */
    private String athleteId;

    /** User.id of the selected trainer */
    private String trainerId;

    /** FITNESS_TRAINING | NUTRITION_COUNSELLING | WELLNESS_COACHING */
    private String serviceType;

    /** ISO date string e.g. "2025-07-01" */
    private String date;

    /** 24-hr time string e.g. "10:00" */
    private String time;

    /** Session length in minutes: 30 | 45 | 60 | 90 */
    private Integer duration;

    /** GYM | ONLINE */
    private String location;

    /** Why the athlete wants this appointment */
    private String purpose;

    /** Any extra info (injuries, preferences, etc.) */
    private String specialRequirements;

    /** PENDING | APPROVED | REJECTED | CANCELLED */
    @Builder.Default
    private String status = "PENDING";

    /** Trainer fills this when approving or rejecting */
    private String trainerRemarks;

    /** Post-session feedback / comments added by the trainer after the appointment */
    private String trainerFeedback;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
