package com.utm.rugbyplanner.service;

import com.utm.rugbyplanner.dto.CopyPlanRequest;
import com.utm.rugbyplanner.dto.PlanEditRequest;
import com.utm.rugbyplanner.dto.PlanProgressRequest;
import com.utm.rugbyplanner.dto.WorkoutPlanRequest;
import com.utm.rugbyplanner.dto.WorkoutPlanResponse;
import com.utm.rugbyplanner.model.User;
import com.utm.rugbyplanner.model.WorkoutPlan;
import com.utm.rugbyplanner.repository.UserRepository;
import com.utm.rugbyplanner.repository.WorkoutPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * WorkoutPlanService — UC004: Create Workout Plan / UC005: Manage Workout Plan
 *
 * Responsibilities:
 *   1. Build a rugby-specific prompt from the user's inputs.
 *   2. Call AiService to generate the plan text.
 *   3. Persist the plan to MongoDB.
 *   4. Provide list / delete operations for UC005.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkoutPlanService {

    private final WorkoutPlanRepository workoutPlanRepository;
    private final UserRepository        userRepository;
    private final AiService             aiService;

    // ── UC004: Generate a new workout plan ───────────────────────────────────

    public WorkoutPlanResponse generatePlan(String username, WorkoutPlanRequest req) {
        User user = findUser(username);

        log.info("UC004 Generate workout plan — user: {}, position: {}, sessions: {}",
                username, req.getRugbyPosition(), req.getSessionsPerWeek());

        // Build rugby-specific prompt
        String prompt = buildPrompt(req);

        // Call Groq (AiService)
        String generatedPlan = aiService.generate(prompt);

        // Persist to MongoDB
        String planName = (req.getPlanName() != null && !req.getPlanName().isBlank())
                ? req.getPlanName()
                : req.getRugbyPosition() + " – " + req.getGoal() + " Plan";

        WorkoutPlan plan = WorkoutPlan.builder()
                .userId(user.getId())
                .planName(planName)
                .rugbyPosition(req.getRugbyPosition())
                .goal(req.getGoal())
                .trainingLevel(req.getTrainingLevel())
                .weight(req.getWeight())
                .height(req.getHeight())
                .age(req.getAge())
                .injuryNotes(req.getInjuryNotes())
                .sessionsPerWeek(req.getSessionsPerWeek())
                .trainingPhase(req.getTrainingPhase())
                .availableEquipment(req.getAvailableEquipment())
                .focusArea(req.getFocusArea())
                .generatedPlan(generatedPlan)
                .build();

        WorkoutPlan saved = workoutPlanRepository.save(plan);
        log.info("UC004 Workout plan saved — id: {}", saved.getId());

        return toResponse(saved);
    }

    // ── UC005: List saved plans ───────────────────────────────────────────────

    public List<WorkoutPlanResponse> getPlans(String username) {
        User user = findUser(username);
        return workoutPlanRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── UC005: Get single plan ────────────────────────────────────────────────

    public WorkoutPlanResponse getPlan(String username, String planId) {
        User user = findUser(username);
        WorkoutPlan plan = workoutPlanRepository
                .findByIdAndUserId(planId, user.getId())
                .orElseThrow(() -> new RuntimeException("Workout plan not found."));
        return toResponse(plan);
    }

    // ── UC005: Delete a plan ──────────────────────────────────────────────────

    public void deletePlan(String username, String planId) {
        User user = findUser(username);
        WorkoutPlan plan = workoutPlanRepository
                .findByIdAndUserId(planId, user.getId())
                .orElseThrow(() -> new RuntimeException("Workout plan not found."));
        workoutPlanRepository.delete(plan);
        log.info("UC005 Workout plan deleted — id: {}", planId);
    }

    // ── UC005: Edit plan name / content ───────────────────────────────────────

    public WorkoutPlanResponse editPlan(String username, String planId, PlanEditRequest req) {
        User user = findUser(username);
        WorkoutPlan plan = workoutPlanRepository
                .findByIdAndUserId(planId, user.getId())
                .orElseThrow(() -> new RuntimeException("Workout plan not found."));
        plan.setPlanName(req.getPlanName());
        plan.setGeneratedPlan(req.getGeneratedPlan());
        WorkoutPlan saved = workoutPlanRepository.save(plan);
        log.info("UC005 Workout plan edited — id: {}", planId);
        return toResponse(saved);
    }

    // ── UC005: Set plan as active (currently in use) ──────────────────────────

    public WorkoutPlanResponse activatePlan(String username, String planId) {
        User user = findUser(username);

        // Deactivate all other plans for this user
        workoutPlanRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .forEach(p -> {
                    if (p.isActive()) {
                        p.setActive(false);
                        workoutPlanRepository.save(p);
                    }
                });

        // Activate the target plan
        WorkoutPlan plan = workoutPlanRepository
                .findByIdAndUserId(planId, user.getId())
                .orElseThrow(() -> new RuntimeException("Workout plan not found."));
        plan.setActive(true);
        WorkoutPlan saved = workoutPlanRepository.save(plan);
        log.info("UC005 Workout plan set as active — id: {}", planId);
        return toResponse(saved);
    }

    // ── UC004 AF2: Copy an existing plan ─────────────────────────────────────

    /**
     * Duplicates a workout plan owned by this user, saving it under a new name.
     * The copy starts inactive with an empty progress list.
     */
    public WorkoutPlanResponse copyPlan(String username, String planId, CopyPlanRequest req) {
        User user = findUser(username);
        WorkoutPlan source = workoutPlanRepository
                .findByIdAndUserId(planId, user.getId())
                .orElseThrow(() -> new RuntimeException("Workout plan not found."));

        WorkoutPlan copy = WorkoutPlan.builder()
                .userId(user.getId())
                .planName(req.getNewName())
                .rugbyPosition(source.getRugbyPosition())
                .goal(source.getGoal())
                .trainingLevel(source.getTrainingLevel())
                .weight(source.getWeight())
                .height(source.getHeight())
                .age(source.getAge())
                .injuryNotes(source.getInjuryNotes())
                .sessionsPerWeek(source.getSessionsPerWeek())
                .trainingPhase(source.getTrainingPhase())
                .availableEquipment(source.getAvailableEquipment())
                .focusArea(source.getFocusArea())
                .generatedPlan(source.getGeneratedPlan())
                // copy starts fresh — not active, no trainer note, no progress
                .isActive(false)
                .completedItems(new java.util.ArrayList<>())
                .build();

        WorkoutPlan saved = workoutPlanRepository.save(copy);
        log.info("UC004 AF2 Workout plan copied — source: {}, new: {}", planId, saved.getId());
        return toResponse(saved);
    }

    // ── UC005: Update progress / checklist ───────────────────────────────────

    public WorkoutPlanResponse updateProgress(String username, String planId, PlanProgressRequest req) {
        User user = findUser(username);
        WorkoutPlan plan = workoutPlanRepository
                .findByIdAndUserId(planId, user.getId())
                .orElseThrow(() -> new RuntimeException("Workout plan not found."));
        plan.setCompletedItems(req.getCompletedItems() != null
                ? req.getCompletedItems() : new java.util.ArrayList<>());
        WorkoutPlan saved = workoutPlanRepository.save(plan);
        log.info("UC005 Workout progress updated — id: {}, completed: {}",
                planId, saved.getCompletedItems().size());
        return toResponse(saved);
    }

    // ── Prompt builder ────────────────────────────────────────────────────────

    /**
     * Builds a detailed, rugby-specific prompt for Groq.
     *
     * The prompt is structured to produce a well-formatted weekly workout
     * plan with day-by-day breakdown, sets/reps, and rugby context.
     */
    private String buildPrompt(WorkoutPlanRequest req) {
        String injuries   = (req.getInjuryNotes() != null && !req.getInjuryNotes().isBlank())
                ? req.getInjuryNotes() : "None reported";
        String focusArea  = (req.getFocusArea() != null && !req.getFocusArea().isBlank())
                ? req.getFocusArea() : "General rugby performance";
        String phase      = phaseLabel(req.getTrainingPhase());
        String equipment  = equipmentLabel(req.getAvailableEquipment());

        return String.format("""
You are an expert rugby strength and conditioning coach specialising in university-level rugby (UTM Pirates, Malaysia).

Generate a detailed %d-day-per-week workout plan for a rugby player with the following profile:

PLAYER PROFILE:
- Position: %s
- Primary Goal: %s
- Training Level: %s
- Physical Stats: %d kg body weight, %d cm height, %d years old
- Sessions Per Week: %d
- Training Phase: %s
- Available Equipment: %s
- Specific Focus / Weakness: %s
- Injury / Health Notes: %s

INSTRUCTIONS:
1. Structure the plan as exactly %d training days (e.g. Day 1, Day 2, ...) with rest/recovery days clearly noted.
2. IMPORTANT — Training phase is %s. Adjust volume, intensity, and exercise selection accordingly:
   - PRE-SEASON: high volume, build base fitness and strength
   - IN-SEASON: maintenance, lower volume, prioritise recovery
   - OFF-SEASON: hypertrophy, address weaknesses, higher intensity
   - POST-SEASON: active recovery, mobility, deload
3. IMPORTANT — Available equipment is %s. Only include exercises that can be performed with this equipment.
4. Prioritise the athlete's specific focus area: %s
5. Each training day must include:
   a) Session Focus (e.g. "Lower Body Strength & Power")
   b) Warm-Up (5–10 minutes, specific movements)
   c) Main Workout (exercises with sets × reps, load guidance as %% of bodyweight or RPE, rest periods).
      The number of exercises in the Main Workout section can vary day to day based on
      what makes sense for that session (e.g. 3 on a lighter day, 5 on a heavier day) —
      there is no fixed count to hit.
   d) Finisher / Conditioning (optional, position-appropriate)
   e) Cool-Down (5 minutes)
   f) Coaching Notes (position-specific tips for %s)
6. Take goal (%s) and training level (%s) into account for volume and exercise selection.
7. IMPORTANT — Injury notes: %s. Avoid aggravating exercises and suggest safe alternatives.
8. Format output using markdown: ## for day headings, ### for sub-sections, bullet points for exercises.
9. End with a "Weekly Structure Summary" table: Day | Focus | Volume | Key Exercises.
   CRITICAL — The "Key Exercises" column for each training day MUST exactly match that
   day's Main Workout section: list every single exercise name from that day's Main
   Workout, comma-separated, in the same order and using the exact same wording as in
   the Main Workout section. The count in this column MUST equal the count of exercises
   in that day's Main Workout — if Monday's Main Workout has 4 exercises, list 4; if
   Tuesday's Main Workout has 5, list 5. Never omit, abbreviate, or summarise this list,
   and never list more or fewer items than actually appear in that day's Main Workout —
   this exact list is used to build the athlete's progress-tracking checklist, so any
   mismatch breaks it. Do not include Finisher/Conditioning exercises in this column,
   only Main Workout ones.

BREVITY (important): the whole plan plus the summary table must fit in a single
response, so be terse. No preamble, no closing commentary, no extra tips beyond the
Coaching Notes asked for above. Keep each bullet to one line. Do not explain your
reasoning. Running long means the plan gets cut off mid-week and is unusable.

Begin the plan now:
""",
                req.getSessionsPerWeek(),
                req.getRugbyPosition(),
                req.getGoal(),
                req.getTrainingLevel(),
                req.getWeight(), req.getHeight(), req.getAge(),
                req.getSessionsPerWeek(),
                phase,
                equipment,
                focusArea,
                injuries,
                req.getSessionsPerWeek(),
                phase,
                equipment,
                focusArea,
                req.getRugbyPosition(),
                req.getGoal(),
                req.getTrainingLevel(),
                injuries
        );
    }

    private String phaseLabel(String phase) {
        if (phase == null) return "General";
        return switch (phase) {
            case "PRE_SEASON"  -> "Pre-Season";
            case "IN_SEASON"   -> "In-Season";
            case "OFF_SEASON"  -> "Off-Season";
            case "POST_SEASON" -> "Post-Season (Deload)";
            default            -> phase;
        };
    }

    private String equipmentLabel(String equipment) {
        if (equipment == null) return "Full gym";
        return switch (equipment) {
            case "GYM"     -> "Full gym (barbells, machines, cables, dumbbells)";
            case "HOME"    -> "Home setup (dumbbells, resistance bands, pull-up bar)";
            case "FIELD"   -> "Rugby field only (bodyweight, cones, tackle bags)";
            case "MINIMAL" -> "Minimal equipment (bodyweight exercises only)";
            default        -> equipment;
        };
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private WorkoutPlanResponse toResponse(WorkoutPlan plan) {
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

    // ── Helper ────────────────────────────────────────────────────────────────

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
