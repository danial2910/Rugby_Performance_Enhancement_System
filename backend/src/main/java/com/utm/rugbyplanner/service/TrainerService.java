package com.utm.rugbyplanner.service;

import com.utm.rugbyplanner.dto.MealPlanResponse;
import com.utm.rugbyplanner.dto.TrainerAthleteResponse;
import com.utm.rugbyplanner.dto.TrainerPlanEditRequest;
import com.utm.rugbyplanner.dto.WorkoutPlanResponse;
import com.utm.rugbyplanner.model.Athlete;
import com.utm.rugbyplanner.model.MealPlan;
import com.utm.rugbyplanner.model.User;
import com.utm.rugbyplanner.model.WorkoutPlan;
import com.utm.rugbyplanner.repository.AthleteRepository;
import com.utm.rugbyplanner.repository.MealPlanRepository;
import com.utm.rugbyplanner.repository.UserRepository;
import com.utm.rugbyplanner.repository.WorkoutPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TrainerService — Trainer plan management use case.
 *
 * Provides trainers with:
 *   1. A list of all registered athletes (with plan counts)
 *   2. Read access to any athlete's workout and meal plans
 *   3. Edit access to any plan — trainer MUST supply a note explaining the change
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainerService {

    private final UserRepository       userRepository;
    private final AthleteRepository    athleteRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final MealPlanRepository    mealPlanRepository;

    // ── Athlete list ──────────────────────────────────────────────────────────

    /**
     * Returns all users with ATHLETE role, enriched with profile data
     * (rugby position) and plan counts.
     */
    public List<TrainerAthleteResponse> getAllAthletes() {
        return userRepository.findAll().stream()
                .filter(u -> u.getUserRole() == User.UserRole.ATHLETE)
                .map(this::toAthleteResponse)
                .collect(Collectors.toList());
    }

    // ── Plans for a specific athlete ──────────────────────────────────────────

    public List<WorkoutPlanResponse> getAthleteWorkoutPlans(String athleteUserId) {
        return workoutPlanRepository
                .findByUserIdOrderByCreatedAtDesc(athleteUserId)
                .stream()
                .map(this::toWorkoutResponse)
                .collect(Collectors.toList());
    }

    public List<MealPlanResponse> getAthleteMealPlans(String athleteUserId) {
        return mealPlanRepository
                .findByUserIdOrderByCreatedAtDesc(athleteUserId)
                .stream()
                .map(this::toMealResponse)
                .collect(Collectors.toList());
    }

    // ── Edit a workout plan ───────────────────────────────────────────────────

    /**
     * Trainer edits a workout plan.
     * Overwrites plan name, generated content, and records the trainer's note
     * and identity for audit/display.
     */
    public WorkoutPlanResponse editWorkoutPlan(String trainerUsername, String planId,
                                               TrainerPlanEditRequest req) {
        User trainer = findUser(trainerUsername);

        WorkoutPlan plan = workoutPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Workout plan not found."));

        plan.setPlanName(req.getPlanName());
        plan.setGeneratedPlan(req.getGeneratedPlan());
        plan.setTrainerNote(req.getTrainerNote());
        plan.setLastEditedBy(trainer.getFullName() != null
                ? trainer.getFullName() : trainer.getUsername());

        WorkoutPlan saved = workoutPlanRepository.save(plan);
        log.info("Trainer {} edited workout plan {} for user {}",
                trainerUsername, planId, saved.getUserId());
        return toWorkoutResponse(saved);
    }

    // ── Edit a meal plan ──────────────────────────────────────────────────────

    public MealPlanResponse editMealPlan(String trainerUsername, String planId,
                                         TrainerPlanEditRequest req) {
        User trainer = findUser(trainerUsername);

        MealPlan plan = mealPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Meal plan not found."));

        plan.setPlanName(req.getPlanName());
        plan.setGeneratedPlan(req.getGeneratedPlan());
        plan.setTrainerNote(req.getTrainerNote());
        plan.setLastEditedBy(trainer.getFullName() != null
                ? trainer.getFullName() : trainer.getUsername());

        MealPlan saved = mealPlanRepository.save(plan);
        log.info("Trainer {} edited meal plan {} for user {}",
                trainerUsername, planId, saved.getUserId());
        return toMealResponse(saved);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private TrainerAthleteResponse toAthleteResponse(User user) {
        Athlete athlete = athleteRepository.findByUserId(user.getId()).orElse(null);
        long workoutCount = workoutPlanRepository.countByUserId(user.getId());
        long mealCount    = mealPlanRepository.countByUserId(user.getId());

        // ── Compute active workout plan progress ──────────────────────────────
        int workoutProgress = 0;
        WorkoutPlan activeWorkout = workoutPlanRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(WorkoutPlan::isActive)
                .findFirst()
                .orElse(null);
        if (activeWorkout != null) {
            int completed = activeWorkout.getCompletedItems() != null
                    ? activeWorkout.getCompletedItems().size() : 0;
            int total = activeWorkout.getSessionsPerWeek() != null
                    ? activeWorkout.getSessionsPerWeek() * 4
                    : Math.max(1, completed);
            if (total > 0) {
                workoutProgress = (int) Math.min(100, Math.round((completed * 100.0) / total));
            }
        }

        // ── Compute active meal plan progress ─────────────────────────────────
        int mealProgress = 0;
        MealPlan activeMeal = mealPlanRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(MealPlan::isActive)
                .findFirst()
                .orElse(null);
        if (activeMeal != null) {
            int completed = activeMeal.getCompletedItems() != null
                    ? activeMeal.getCompletedItems().size() : 0;
            int total = activeMeal.getMealsPerDay() != null
                    ? activeMeal.getMealsPerDay() * 7
                    : Math.max(1, completed);
            if (total > 0) {
                mealProgress = (int) Math.min(100, Math.round((completed * 100.0) / total));
            }
        }

        return TrainerAthleteResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .matrixNumber(user.getMatrixNumber())
                .rugbyPosition(athlete != null ? athlete.getRugbyPosition() : null)
                .workoutPlanCount(workoutCount)
                .mealPlanCount(mealCount)
                .workoutProgress(workoutProgress)
                .mealProgress(mealProgress)
                .joinedAt(user.getCreatedAt())
                .build();
    }

    private WorkoutPlanResponse toWorkoutResponse(WorkoutPlan plan) {
        return WorkoutPlanResponse.builder()
                .id(plan.getId())
                .userId(plan.getUserId())
                .planName(plan.getPlanName())
                .rugbyPosition(plan.getRugbyPosition())
                .goal(plan.getGoal())
                .trainingLevel(plan.getTrainingLevel())
                .weight(plan.getWeight())
                .height(plan.getHeight())
                .age(plan.getAge())
                .injuryNotes(plan.getInjuryNotes())
                .sessionsPerWeek(plan.getSessionsPerWeek())
                .trainingPhase(plan.getTrainingPhase())
                .availableEquipment(plan.getAvailableEquipment())
                .focusArea(plan.getFocusArea())
                .generatedPlan(plan.getGeneratedPlan())
                .trainerNote(plan.getTrainerNote())
                .lastEditedBy(plan.getLastEditedBy())
                .isActive(plan.isActive())
                .completedItems(plan.getCompletedItems())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }

    private MealPlanResponse toMealResponse(MealPlan plan) {
        return MealPlanResponse.builder()
                .id(plan.getId())
                .userId(plan.getUserId())
                .planName(plan.getPlanName())
                .rugbyPosition(plan.getRugbyPosition())
                .goals(plan.getGoals())
                .weight(plan.getWeight())
                .height(plan.getHeight())
                .age(plan.getAge())
                .dietaryPreference(plan.getDietaryPreference())
                .allergies(plan.getAllergies())
                .mealsPerDay(plan.getMealsPerDay())
                .activityLevel(plan.getActivityLevel())
                .targetWeight(plan.getTargetWeight())
                .trainingPhase(plan.getTrainingPhase())
                .mealPrepTime(plan.getMealPrepTime())
                .generatedPlan(plan.getGeneratedPlan())
                .trainerNote(plan.getTrainerNote())
                .lastEditedBy(plan.getLastEditedBy())
                .isActive(plan.isActive())
                .completedItems(plan.getCompletedItems())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
