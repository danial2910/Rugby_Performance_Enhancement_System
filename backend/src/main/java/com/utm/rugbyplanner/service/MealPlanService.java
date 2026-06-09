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
 *   3. Call OllamaService to generate the plan text.
 *   4. Persist the plan to MongoDB.
 *   5. Provide list / delete operations for UC007.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final UserRepository     userRepository;
    private final OllamaService      ollamaService;

    // ── UC006: Generate a new meal plan ──────────────────────────────────────

    public MealPlanResponse generatePlan(String username, MealPlanRequest req) {
        User user = findUser(username);

        log.info("UC006 Generate meal plan — user: {}, position: {}, goals: {}, diet: {}",
                username, req.getRugbyPosition(), req.getGoals(), req.getDietaryPreference());

        String prompt = buildPrompt(req);
        String generatedPlan = ollamaService.generate(prompt);

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

        return String.format("""
You are an expert sports nutritionist specialising in rugby performance nutrition for university athletes (UTM Pirates, Malaysia).

Generate a complete 7-day meal plan for a rugby player with the following profile:

PLAYER PROFILE:
- Position: %s
- Nutrition Goals: %s
- Physical Stats: %d kg current weight, %d cm height, %d years old
- Target Weight: %s
- Estimated Daily Calorie Target: ~%d kcal/day (based on %s activity level)
- Training Phase: %s
- Dietary Preference: %s
- Food Allergies / Intolerances: %s
- Meals Per Day: %d
- Meal Prep Time Available: %s

INSTRUCTIONS:
1. Create a structured plan for Day 1 (Monday) through Day 7 (Sunday).
2. IMPORTANT — Training phase is %s. Adjust nutrition strategy accordingly:
   - PRE-SEASON: high carbohydrates for energy-demanding training, moderate protein
   - IN-SEASON: performance nutrition, carb-timing around matches and training
   - OFF-SEASON: suit the goal (surplus for muscle gain, deficit for weight loss)
   - POST-SEASON: recovery focus, anti-inflammatory foods, moderate calories
3. Meal prep time is %s. Keep meals practical for this constraint:
   - LOW (under 15 min): simple, ready-to-eat or minimal cooking
   - MEDIUM (15–30 min): standard cooking, batch-prep friendly
   - HIGH (30+ min): full recipes, complex preparations are acceptable
4. Each day must include exactly %d meals clearly labelled.
5. For each meal provide:
   a) Meal name / description
   b) Specific foods with portion sizes (grams or standard Malaysian measures)
   c) Estimated macros: Protein (g), Carbs (g), Fat (g), Calories (kcal)
6. Strictly follow the dietary preference (%s).
7. IMPORTANT — Allergy consideration: %s. Never include these ingredients.
8. Use locally available Malaysian foods where possible (nasi lemak, roti canai, ikan bakar, ayam goreng, etc.).
9. Vary meals across 7 days — avoid repeating the same meal more than twice.
10. On training-heavy days increase carbohydrate loading by 15–20%% for energy.
11. On recovery days emphasise anti-inflammatory foods and higher protein.
12. End with a "Weekly Nutrition Summary" table: Day | Calories | Protein | Carbs | Fat.
13. Format output using markdown: ## for day headings, ### for meal names, bullet points for food items.

Begin the 7-day meal plan now:
""",
                req.getRugbyPosition(),
                goalsText,
                req.getWeight(), req.getHeight(), req.getAge(),
                targetWeightStr,
                targetCalories,
                req.getActivityLevel(),
                phase,
                req.getDietaryPreference(),
                allergies,
                req.getMealsPerDay(),
                prepTime,
                phase,
                prepTime,
                req.getMealsPerDay(),
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
