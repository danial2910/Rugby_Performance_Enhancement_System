package com.utm.rugbyplanner.service;

import com.utm.rugbyplanner.dto.*;
import com.utm.rugbyplanner.model.Appointment;
import com.utm.rugbyplanner.model.Trainer;
import com.utm.rugbyplanner.model.User;
import com.utm.rugbyplanner.repository.AppointmentRepository;
import com.utm.rugbyplanner.repository.TrainerRepository;
import com.utm.rugbyplanner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * AppointmentService
 *
 * UC008: Create Appointment  — athlete books a session with a trainer
 * UC009: Manage Appointment  — athlete views their appointments and can cancel PENDING ones
 * UC012: Approve Appointment — trainer approves or rejects PENDING requests
 *
 * Also provides:
 *   - Trainer listing endpoint used by the booking form
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository        userRepository;
    private final TrainerRepository     trainerRepository;
    private final EmailService          emailService;

    // ── Trainer listing (used by booking form) ────────────────────────────────

    /**
     * Returns all users with TRAINER role, enriched with profile data
     * (expertise, experience, availability slots) for display on booking form.
     */
    public List<TrainerSummaryResponse> getAvailableTrainers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getUserRole() == User.UserRole.TRAINER)
                .map(this::toTrainerSummary)
                .collect(Collectors.toList());
    }

    // ── UC008: Create appointment ─────────────────────────────────────────────

    public AppointmentResponse createAppointment(String athleteUsername, AppointmentRequest req) {
        User athlete = findUser(athleteUsername);

        // Verify the selected trainer exists and is actually a trainer
        User trainerUser = userRepository.findById(req.getTrainerId())
                .orElseThrow(() -> new RuntimeException("Trainer not found."));
        if (trainerUser.getUserRole() != User.UserRole.TRAINER) {
            throw new RuntimeException("Selected user is not a trainer.");
        }

        // ── Availability slot validation ──────────────────────────────────────
        validateTrainerAvailability(req.getTrainerId(), req.getDate(), req.getTime(), req.getDuration());

        Appointment appointment = Appointment.builder()
                .athleteId(athlete.getId())
                .trainerId(req.getTrainerId())
                .serviceType(req.getServiceType())
                .date(req.getDate())
                .time(req.getTime())
                .duration(req.getDuration())
                .location(req.getLocation())
                .purpose(req.getPurpose())
                .specialRequirements(req.getSpecialRequirements())
                .status("PENDING")
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        log.info("UC008 Appointment created — id: {}, athlete: {}, trainer: {}",
                saved.getId(), athlete.getUsername(), trainerUser.getUsername());

        return toResponse(saved, athlete, trainerUser);
    }

    // ── Availability validation helper ────────────────────────────────────────

    /**
     * Validates that the requested date+time falls within one of the trainer's
     * configured availability slots, AND that the session end time does not
     * exceed the slot's end time.
     *
     * Slot day names are stored as full English names (e.g. "MONDAY") or
     * abbreviations (e.g. "Mon") — both are handled case-insensitively.
     */
    private void validateTrainerAvailability(String trainerId, String dateStr,
                                              String timeStr, Integer durationMins) {
        Trainer trainerProfile = trainerRepository.findByUserId(trainerId).orElse(null);

        // If the trainer has no profile or no slots set, skip validation
        if (trainerProfile == null
                || trainerProfile.getAvailabilitySlots() == null
                || trainerProfile.getAvailabilitySlots().isEmpty()) {
            return;
        }

        // Derive the day-of-week name from the requested date (e.g. "MONDAY")
        LocalDate date        = LocalDate.parse(dateStr);
        String    requestedDay = date.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                .toUpperCase();  // e.g. "MONDAY"

        LocalTime requestedStart = LocalTime.parse(timeStr);
        LocalTime requestedEnd   = requestedStart.plusMinutes(durationMins != null ? durationMins : 0);

        // Find a slot matching the day
        Trainer.AvailabilitySlot matchedSlot = trainerProfile.getAvailabilitySlots().stream()
                .filter(slot -> slot.getDay() != null &&
                        slot.getDay().toUpperCase().startsWith(requestedDay.substring(0, 3)))
                .findFirst()
                .orElse(null);

        if (matchedSlot == null) {
            // Build a human-readable list of available days
            String availableDays = trainerProfile.getAvailabilitySlots().stream()
                    .map(s -> capitalize(s.getDay().toLowerCase()))
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("none");
            throw new RuntimeException(
                    "This trainer is not available on " + capitalize(requestedDay.toLowerCase()) +
                    ". Available days: " + availableDays + ".");
        }

        // Check time falls within slot
        LocalTime slotStart = LocalTime.parse(matchedSlot.getStartTime());
        LocalTime slotEnd   = LocalTime.parse(matchedSlot.getEndTime());

        if (requestedStart.isBefore(slotStart) || requestedEnd.isAfter(slotEnd)) {
            throw new RuntimeException(
                    "This trainer is only available between " +
                    matchedSlot.getStartTime() + " and " + matchedSlot.getEndTime() +
                    " on " + capitalize(requestedDay.toLowerCase()) +
                    ". Please choose a time within that window.");
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ── UC009: Edit appointment (athlete) ─────────────────────────────────────

    public AppointmentResponse editAppointment(String athleteUsername, String appointmentId,
                                                AppointmentRequest req) {
        User athlete = findUser(athleteUsername);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found."));

        if (!appointment.getAthleteId().equals(athlete.getId())) {
            throw new RuntimeException("You can only edit your own appointments.");
        }
        if (!"PENDING".equals(appointment.getStatus())) {
            throw new RuntimeException("Only PENDING appointments can be edited.");
        }

        // Verify the trainer still exists and is a trainer
        User trainerUser = userRepository.findById(req.getTrainerId())
                .orElseThrow(() -> new RuntimeException("Trainer not found."));
        if (trainerUser.getUserRole() != User.UserRole.TRAINER) {
            throw new RuntimeException("Selected user is not a trainer.");
        }

        // Re-validate availability for the new date/time/trainer
        validateTrainerAvailability(req.getTrainerId(), req.getDate(), req.getTime(), req.getDuration());

        appointment.setTrainerId(req.getTrainerId());
        appointment.setServiceType(req.getServiceType());
        appointment.setDate(req.getDate());
        appointment.setTime(req.getTime());
        appointment.setDuration(req.getDuration());
        appointment.setLocation(req.getLocation());
        appointment.setPurpose(req.getPurpose());
        appointment.setSpecialRequirements(req.getSpecialRequirements());

        Appointment saved = appointmentRepository.save(appointment);
        log.info("UC009 Appointment edited — id: {}, athlete: {}", appointmentId, athleteUsername);

        return toResponse(saved, athlete, trainerUser);
    }

    // ── UC009: List athlete's appointments ────────────────────────────────────

    public List<AppointmentResponse> getAthleteAppointments(String athleteUsername) {
        User athlete = findUser(athleteUsername);
        return appointmentRepository
                .findByAthleteIdOrderByCreatedAtDesc(athlete.getId())
                .stream()
                .map(a -> {
                    User trainerUser = userRepository.findById(a.getTrainerId()).orElse(null);
                    return toResponse(a, athlete, trainerUser);
                })
                .collect(Collectors.toList());
    }

    // ── Delete appointment (athlete — own record) ─────────────────────────────

    /**
     * Permanently removes an appointment record that belongs to this athlete.
     * The athlete may delete any appointment regardless of status, so they can
     * clean up old CANCELLED, REJECTED, or past APPROVED sessions.
     */
    public void deleteAppointmentAsAthlete(String athleteUsername, String appointmentId) {
        User athlete = findUser(athleteUsername);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found."));
        if (!appointment.getAthleteId().equals(athlete.getId())) {
            throw new RuntimeException("You can only delete your own appointments.");
        }
        appointmentRepository.delete(appointment);
        log.info("Appointment deleted by athlete — id: {}, athlete: {}", appointmentId, athleteUsername);
    }

    // ── Delete appointment (trainer — own record) ─────────────────────────────

    /**
     * Permanently removes an appointment record that belongs to this trainer.
     * The trainer may delete any appointment regardless of status.
     */
    public void deleteAppointmentAsTrainer(String trainerUsername, String appointmentId) {
        User trainer = findUser(trainerUsername);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found."));
        if (!appointment.getTrainerId().equals(trainer.getId())) {
            throw new RuntimeException("You can only delete appointments assigned to you.");
        }
        appointmentRepository.delete(appointment);
        log.info("Appointment deleted by trainer — id: {}, trainer: {}", appointmentId, trainerUsername);
    }

    // ── UC009: Cancel appointment (athlete) ───────────────────────────────────

    public AppointmentResponse cancelAppointment(String athleteUsername, String appointmentId) {
        User athlete = findUser(athleteUsername);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found."));

        if (!appointment.getAthleteId().equals(athlete.getId())) {
            throw new RuntimeException("You can only cancel your own appointments.");
        }
        if (!"PENDING".equals(appointment.getStatus())) {
            throw new RuntimeException("Only PENDING appointments can be cancelled.");
        }

        appointment.setStatus("CANCELLED");
        Appointment saved = appointmentRepository.save(appointment);
        log.info("UC009 Appointment cancelled — id: {}, athlete: {}", appointmentId, athleteUsername);

        User trainerUser = userRepository.findById(saved.getTrainerId()).orElse(null);
        return toResponse(saved, athlete, trainerUser);
    }

    // ── UC012: Trainer views their appointments ───────────────────────────────

    public List<AppointmentResponse> getTrainerAppointments(String trainerUsername) {
        User trainer = findUser(trainerUsername);
        return appointmentRepository
                .findByTrainerIdOrderByCreatedAtDesc(trainer.getId())
                .stream()
                .map(a -> {
                    User athleteUser = userRepository.findById(a.getAthleteId()).orElse(null);
                    return toResponse(a, athleteUser, trainer);
                })
                .collect(Collectors.toList());
    }

    // ── UC012: Approve appointment ────────────────────────────────────────────

    public AppointmentResponse approveAppointment(String trainerUsername, String appointmentId,
                                                   AppointmentRemarkRequest req) {
        User trainer = findUser(trainerUsername);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found."));

        if (!appointment.getTrainerId().equals(trainer.getId())) {
            throw new RuntimeException("You can only manage your own appointments.");
        }
        if (!"PENDING".equals(appointment.getStatus())) {
            throw new RuntimeException("Only PENDING appointments can be approved.");
        }

        appointment.setStatus("APPROVED");
        if (req != null && req.getTrainerRemarks() != null) {
            appointment.setTrainerRemarks(req.getTrainerRemarks());
        }

        Appointment saved = appointmentRepository.save(appointment);
        log.info("UC012 Appointment approved — id: {}, trainer: {}", appointmentId, trainerUsername);

        User athleteUser = userRepository.findById(saved.getAthleteId()).orElse(null);

        // UC010: Send email notification to athlete
        if (athleteUser != null && athleteUser.getEmail() != null) {
            String athleteName = athleteUser.getFullName() != null ? athleteUser.getFullName() : athleteUser.getUsername();
            String trainerName = trainer.getFullName() != null ? trainer.getFullName() : trainer.getUsername();
            emailService.sendAppointmentApprovedEmail(athleteUser.getEmail(), athleteName, trainerName, saved);
        }

        return toResponse(saved, athleteUser, trainer);
    }

    // ── UC012: Reject appointment ─────────────────────────────────────────────

    public AppointmentResponse rejectAppointment(String trainerUsername, String appointmentId,
                                                  AppointmentRemarkRequest req) {
        User trainer = findUser(trainerUsername);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found."));

        if (!appointment.getTrainerId().equals(trainer.getId())) {
            throw new RuntimeException("You can only manage your own appointments.");
        }
        if (!"PENDING".equals(appointment.getStatus())) {
            throw new RuntimeException("Only PENDING appointments can be rejected.");
        }

        appointment.setStatus("REJECTED");
        if (req != null && req.getTrainerRemarks() != null) {
            appointment.setTrainerRemarks(req.getTrainerRemarks());
        }

        Appointment saved = appointmentRepository.save(appointment);
        log.info("UC012 Appointment rejected — id: {}, trainer: {}", appointmentId, trainerUsername);

        User athleteUser = userRepository.findById(saved.getAthleteId()).orElse(null);

        // UC010: Send email notification to athlete
        if (athleteUser != null && athleteUser.getEmail() != null) {
            String athleteName = athleteUser.getFullName() != null ? athleteUser.getFullName() : athleteUser.getUsername();
            String trainerName = trainer.getFullName() != null ? trainer.getFullName() : trainer.getUsername();
            emailService.sendAppointmentRejectedEmail(athleteUser.getEmail(), athleteName, trainerName, saved);
        }

        return toResponse(saved, athleteUser, trainer);
    }

    // ── UC012: Add post-session feedback ─────────────────────────────────────

    /**
     * Trainer adds feedback/comments on an APPROVED appointment after the session.
     * Can be called multiple times — each call overwrites the previous feedback.
     */
    public AppointmentResponse addFeedback(String trainerUsername, String appointmentId,
                                           AppointmentFeedbackRequest req) {
        User trainer = findUser(trainerUsername);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found."));

        if (!appointment.getTrainerId().equals(trainer.getId())) {
            throw new RuntimeException("You can only add feedback to your own appointments.");
        }
        if (!"APPROVED".equals(appointment.getStatus())) {
            throw new RuntimeException("Feedback can only be added to APPROVED appointments.");
        }

        appointment.setTrainerFeedback(req.getTrainerFeedback());
        Appointment saved = appointmentRepository.save(appointment);
        log.info("UC012 Trainer feedback added — id: {}, trainer: {}", appointmentId, trainerUsername);

        User athleteUser = userRepository.findById(saved.getAthleteId()).orElse(null);
        return toResponse(saved, athleteUser, trainer);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private AppointmentResponse toResponse(Appointment a, User athlete, User trainer) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .athleteId(a.getAthleteId())
                .athleteName(athlete != null
                        ? (athlete.getFullName() != null ? athlete.getFullName() : athlete.getUsername())
                        : "Unknown Athlete")
                .trainerId(a.getTrainerId())
                .trainerName(trainer != null
                        ? (trainer.getFullName() != null ? trainer.getFullName() : trainer.getUsername())
                        : "Unknown Trainer")
                .serviceType(a.getServiceType())
                .date(a.getDate())
                .time(a.getTime())
                .duration(a.getDuration())
                .location(a.getLocation())
                .purpose(a.getPurpose())
                .specialRequirements(a.getSpecialRequirements())
                .status(a.getStatus())
                .trainerRemarks(a.getTrainerRemarks())
                .trainerFeedback(a.getTrainerFeedback())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    private TrainerSummaryResponse toTrainerSummary(User user) {
        Trainer profile = trainerRepository.findByUserId(user.getId()).orElse(null);
        return TrainerSummaryResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName() != null ? user.getFullName() : user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .profilePicture(user.getProfilePicture())
                .expertise(profile != null ? profile.getExpertise() : null)
                .experience(profile != null ? profile.getExperience() : null)
                .availabilitySlots(profile != null ? profile.getAvailabilitySlots() : null)
                .build();
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
