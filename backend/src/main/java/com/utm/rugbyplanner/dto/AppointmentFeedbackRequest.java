package com.utm.rugbyplanner.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AppointmentFeedbackRequest — body for PUT /api/appointments/{id}/feedback
 *
 * Allows a trainer to add post-session feedback/comments on an APPROVED appointment.
 */
@Data
public class AppointmentFeedbackRequest {

    @NotBlank(message = "Feedback cannot be empty.")
    private String trainerFeedback;
}
