package com.utm.rugbyplanner.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * AppointmentResponse — returned for all appointment read/write operations.
 * Includes denormalised trainer and athlete display names so the UI
 * doesn't need extra lookups.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {

    private String id;

    // ── Participant info ───────────────────────────────────────────────────
    private String athleteId;
    private String athleteName;

    private String trainerId;
    private String trainerName;

    // ── Booking details ────────────────────────────────────────────────────
    private String serviceType;
    private String date;
    private String time;
    private Integer duration;
    private String location;
    private String purpose;
    private String specialRequirements;

    // ── Status ─────────────────────────────────────────────────────────────
    private String status;
    private String trainerRemarks;
    private String trainerFeedback;

    // ── Timestamps ─────────────────────────────────────────────────────────
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
