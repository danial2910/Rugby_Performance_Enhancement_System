package com.utm.rugbyplanner.service;

import com.utm.rugbyplanner.dto.CopyPlanRequest;
import com.utm.rugbyplanner.dto.MealPlanRequest;
import com.utm.rugbyplanner.dto.MealPlanResponse;
import com.utm.rugbyplanner.dto.PlanEditRequest;
import com.utm.rugbyplanner.dto.PlanProgressRequest;
import com.utm.rugbyplanner.model.MealPlan;
import com.utm.rugbyplanner.model.User;
import com.utm.rugbyplanner.repository.MealPlanRepository;
import com.utm.rugbyplanner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MealPlanService — UC006: Create Meal Plan / UC007: Manage Meal Plan
 *
 * Responsibilities:
 *   1. Build a rugby-specific 7-day meal prompt from the user's inputs.
 *   2. Estimate the user's TDEE based on weight, height, age and goal.
 *   3. Call AiService to generate the plan text.
 *   4. Persist the plan to MongoDB.
 *   5. Provide list / delete operations for UC007.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final UserRepository     userRepository;
    private final AiService          aiService;

    // ── UC006: Generate a new meal plan ──────────────────────────────────────

    public MealPlanResponse generatePlan(String username, MealPlanRequest req) {
        User user = findUser(username);

        log.info("UC006 Generate meal plan — user: {}, position: {}, goals: {}, diet: {}",
                username, req.getRugbyPosition(), req.getGoals(), req.getDietaryPreference());

        String prompt = buildPrompt(req);
        // The prompt now requires the AI to show its addition work for each
        // day's totals (e.g. "350+275+450 = 1075 kcal") before writing the
        // Weekly Nutrition Summary table, and to copy those same numbers into
        // the table rather than recomputing them. There is no backend
        // recalculation of these totals — we rely on the AI's shown work.
        String generatedPlan = aiService.generate(prompt);

        String goalsLabel = req.getGoals().stream()
                .map(this::goalLabel)
                .collect(Collectors.joining(" + "));
        String planName = (req.getPlanName() != null && !req.getPlanName().isBlank())
                ? req.getPlanName()
                : req.getRugbyPosition() + " – " + goalsLabel + " Meal Plan";

        MealPlan plan = MealPlan.builder()
                .userId(user.getId())
                .planName(planName)
                .rugbyPosition(req.getRugbyPosition())
                .goals(req.getGoals())
                .weight(req.getWeight())
                .height(req.getHeight())
                .age(req.getAge())
                .dietaryPreference(req.getDietaryPreference())
                .allergies(req.getAllergies())
                .mealsPerDay(req.getMealsPerDay())
                .activityLevel(req.getActivityLevel())
                .targetWeight(req.getTargetWeight())
                .trainingPhase(req.getTrainingPhase())
                .mealPrepTime(req.getMealPrepTime())
                .generatedPlan(generatedPlan)
                .build();

        MealPlan saved = mealPlanRepository.save(plan);
        log.info("UC006 Meal plan saved — id: {}", saved.getId());

        return toResponse(saved);
    }

    // ── UC007: List saved plans ───────────────────────────────────────────────

    public List<MealPlanResponse> getPlans(String username) {
        User user = findUser(username);
        return mealPlanRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── UC007: Get single plan ────────────────────────────────────────────────

    public MealPlanResponse getPlan(String username, String planId) {
        User user = findUser(username);
        MealPlan plan = mealPlanRepository
                .findByIdAndUserId(planId, user.getId())
                .orElseThrow(() -> new RuntimeException("Meal plan not found."));
        return toResponse(plan);
    }

    // ── UC007: Delete a plan ──────────────────────────────────────────────────

    public void deletePlan(String username, String planId) {
        User user = findUser(username);
        MealPlan plan = mealPlanRepository
                .findByIdAndUserId(planId, user.getId())
                .orElseThrow(() -> new RuntimeException("Meal plan not found."));
        mealPlanRepository.delete(plan);
        log.info("UC007 Meal plan deleted — id: {}", planId);
    }

    // ── UC007: Edit plan name / content ───────────────────────────────────────

    public MealPlanResponse editPlan(String username, String planId, PlanEditRequest req) {
        User user = findUser(username);
        MealPlan plan = mealPlanRepository
                .findByIdAndUserId(planId, user.getId())
                .orElseThrow(() -> new RuntimeException("Meal plan not found."));
        plan.setPlanName(req.getPlanName());
        plan.setGeneratedPlan(req.getGeneratedPlan());
        MealPlan saved = mealPlanRepository.save(plan);
        log.info("UC007 Meal plan edited — id: {}", planId);
        return toResponse(saved);
    }

    // ── UC007: Set plan as active (currently in use) ──────────────────────────

    public MealPlanResponse activatePlan(String username, String planId) {
        User user = findUser(username);

        // Deactivate all other plans for this user
        mealPlanRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .forEach(p -> {
                    if (p.isActive()) {
                        p.setActive(false);
                        mealPlanRepository.save(p);
                    }
                });

        // Activate the target plan
        MealPlan plan = mealPlanRepository
                .findByIdAndUserId(planId, user.getId())
                .orElseThrow(() -> new RuntimeException("Meal plan not found."));
        plan.setActive(true);
        MealPlan saved = mealPlanRepository.save(plan);
        log.info("UC007 Meal plan set as active — id: {}", planId);
        return toResponse(saved);
    }

    // ── UC006 AF2: Copy an existing plan ─────────────────────────────────────

    /**
     * Duplicates a meal plan owned by this user, saving it under a new name.
     * The copy starts inactive with an empty progress list.
     */
    public MealPlanResponse copyPlan(String username, String planId, CopyPlanRequest req) {
        User user = findUser(username);
        MealPlan source = mealPlanRepository
                .findByIdAndUserId(planId, user.getId())
                .orElseThrow(() -> new RuntimeException("Meal plan not found."));

        MealPlan copy = MealPlan.builder()
                .userId(user.getId())
                .planName(req.getNewName())
                .rugbyPosition(source.getRugbyPosition())
                .goals(source.getGoals())
                .weight(source.getWeight())
                .height(source.getHeight())
                .age(source.getAge())
                .dietaryPreference(source.getDietaryPreference())
                .allergies(source.getAllergies())
                .mealsPerDay(source.getMealsPerDay())
                .activityLevel(source.getActivityLevel())
                .targetWeight(source.getTargetWeight())
                .trainingPhase(source.getTrainingPhase())
                .mealPrepTime(source.getMealPrepTime())
                .generatedPlan(source.getGeneratedPlan())
                // copy starts fresh — not active, no trainer note, no progress
                .isActive(false)
                .completedItems(new java.util.ArrayList<>())
                .build();

        MealPlan saved = mealPlanRepository.save(copy);
        log.info("UC006 AF2 Meal plan copied — source: {}, new: {}", planId, saved.getId());
        return toResponse(saved);
    }

    // ── UC007: Update progress / checklist ───────────────────────────────────

    public MealPlanResponse updateProgress(String username, String planId, PlanProgressRequest req) {
        User user = findUser(username);
        MealPlan plan = mealPlanRepository
                .findByIdAndUserId(planId, user.getId())
                .orElseThrow(() -> new RuntimeException("Meal plan not found."));
        plan.setCompletedItems(req.getCompletedItems() != null
                ? req.getCompletedItems() : new java.util.ArrayList<>());
        MealPlan saved = mealPlanRepository.save(plan);
        log.info("UC007 Meal progress updated — id: {}, completed: {}",
                planId, saved.getCompletedItems().size());
        return toResponse(saved);
    }

    // ── Prompt builder ────────────────────────────────────────────────────────

    private String buildPrompt(MealPlanRequest req) {
        String allergies     = (req.getAllergies() != null && !req.getAllergies().isBlank())
                ? req.getAllergies() : "None";
        String targetWeightStr = (req.getTargetWeight() != null)
                ? req.getTargetWeight() + " kg" : "Not specified";
        String phase         = phaseLabel(req.getTrainingPhase());
        String prepTime      = prepLabel(req.getMealPrepTime());

        // Accurate TDEE using activity level; use first goal for calorie adjustment
        int tdee           = estimateTdee(req.getWeight(), req.getHeight(), req.getAge(), req.getActivityLevel());
        String primaryGoal = req.getGoals().isEmpty() ? "MAINTAIN" : req.getGoals().get(0);
        int targetCalories = adjustCaloriesForGoal(tdee, primaryGoal);
        String goalsText   = req.getGoals().stream()
                .map(this::goalLabel)
                .collect(Collectors.joining(", "));
        // The AI was reliably landing at roughly 40% of the daily calorie target
        // (e.g. ~1635 kcal vs a ~4035 kcal target) even with an explicit ±10%
        // instruction, because an abstract day-level percentage gives it nothing
        // concrete to size individual meals against. Giving it a per-meal kcal
        // figure (daily target / meals per day) gives it a portion-sizing anchor
        // for every single meal, which is much more effective at steering output.
        int mealsPerDayForCalc = req.getMealsPerDay() > 0 ? req.getMealsPerDay() : 3;
        int mealCalorieTarget  = Math.round((float) targetCalories / mealsPerDayForCalc);

        return String.format("""
You are a sports nutritionist for rugby athletes (UTM Pirates, Malaysia).

Generate a 7-day meal plan (Day 1 Monday – Day 7 Sunday).

PLAYER PROFILE:
- Position: %s | Goals: %s
- Stats: %d kg, %d cm, %d years old | Target Weight: %s
- Daily Calorie Target: ~%d kcal/day (%s activity) | ~%d kcal per meal
- Training Phase: %s | Diet: %s | Allergies: %s
- Meals Per Day: %d | Prep Time: %s

RULES:
1. Each day has exactly %d meals. Each meal averages ~%d kcal so daily total reaches ~%d kcal (±10%%).
2. Use generous, calorie-dense portions — do not under-portion.
3. List each food with its portion in grams on one line. Do NOT give per-item macros — give macros ONCE per meal, on that meal's Total line (Protein, Carbs, Fat, kcal).
4. After each day's meals add: Daily Total: Xg Protein, Xg Carbs, Xg Fat, X kcal.
5. Diet: %s. Allergies: %s — never include these.
6. Use Malaysian foods (nasi lemak, roti canai, ikan bakar, ayam goreng, etc.).
7. Vary meals across 7 days. Increase carbs on training days, emphasise protein on recovery days.
8. End with a Weekly Nutrition Summary table: Day | Calories | Protein | Carbs | Fat.

BREVITY (important): the whole 7-day plan plus the summary table must fit in a single
response, so be terse. No preamble, no closing commentary, no tips or notes, no bold or
italic decoration. Do not explain your reasoning. Output only the plan in the exact
format below. Running long means the plan gets cut off mid-week and is unusable.

FORMAT (follow exactly):
## Day 1 (Monday)
### Meal 1: Breakfast
- Nasi lemak 250g
- Boiled egg 2 large
Total: 35g Protein, 90g Carbs, 28g Fat, 780kcal

Begin now:
""",
                req.getRugbyPosition(),
                goalsText,
                req.getWeight(), req.getHeight(), req.getAge(),
                targetWeightStr,
                targetCalories,
                req.getActivityLevel(),
                mealCalorieTarget,
                phase,
                req.getDietaryPreference(),
                allergies,
                req.getMealsPerDay(),
                prepTime,
                req.getMealsPerDay(),
                mealCalorieTarget,
                targetCalories,
                req.getDietaryPreference(),
                allergies
        );
    }

    // ── TDEE estimation (Mifflin-St Jeor + activity multiplier) ─────────────

    /**
     * TDEE estimate using Mifflin-St Jeor BMR × activity multiplier.
     * Activity level maps to standard multipliers:
     *   MODERATE → 1.55 (moderately active, 3–5 days/week)
     *   ACTIVE   → 1.725 (very active, 6–7 days/week)
     *   EXTREME  → 1.9  (extra active, twice-a-day training)
     */
    private int estimateTdee(int weight, int height, int age, String activityLevel) {
        double bmr = (10.0 * weight) + (6.25 * height) - (5.0 * age) + 5;
        double multiplier = switch (activityLevel != null ? activityLevel : "ACTIVE") {
            case "MODERATE" -> 1.55;
            case "EXTREME"  -> 1.9;
            default         -> 1.725; // ACTIVE
        };
        return (int) Math.round(bmr * multiplier);
    }

    private int adjustCaloriesForGoal(int tdee, String goal) {
        return switch (goal) {
            case "MUSCLE_GAIN"  -> tdee + 400;
            case "WEIGHT_LOSS"  -> tdee - 400;
            case "PERFORMANCE"  -> tdee + 200;
            default             -> tdee;
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String goalLabel(String goal) {
        return switch (goal) {
            case "MUSCLE_GAIN"  -> "Muscle Gain";
            case "WEIGHT_LOSS"  -> "Weight Loss";
            case "PERFORMANCE"  -> "Performance";
            default             -> "Maintenance";
        };
    }

    private String phaseLabel(String phase) {
        if (phase == null) return "General";
        return switch (phase) {
            case "PRE_SEASON"  -> "Pre-Season";
            case "IN_SEASON"   -> "In-Season";
            case "OFF_SEASON"  -> "Off-Season";
            case "POST_SEASON" -> "Post-Season (Recovery)";
            default            -> phase;
        };
    }

    private String prepLabel(String prep) {
        if (prep == null) return "Medium (15–30 min)";
        return switch (prep) {
            case "LOW"    -> "Low (under 15 min — quick and simple meals)";
            case "HIGH"   -> "High (30+ min — full recipes acceptable)";
            default       -> "Medium (15–30 min — standard cooking)";
        };
    }

    private MealPlanResponse toResponse(MealPlan plan) {
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
