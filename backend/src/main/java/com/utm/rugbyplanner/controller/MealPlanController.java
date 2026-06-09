package com.utm.rugbyplanner.controller;

import com.utm.rugbyplanner.dto.ApiResponse;
import com.utm.rugbyplanner.dto.CopyPlanRequest;
import com.utm.rugbyplanner.dto.MealPlanRequest;
import com.utm.rugbyplanner.dto.MealPlanResponse;
import com.utm.rugbyplanner.dto.PlanEditRequest;
import com.utm.rugbyplanner.dto.PlanProgressRequest;
import com.utm.rugbyplanner.service.MealPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MealPlanController
 *
 * UC006: Create Meal Plan
 * UC007: Manage Meal Plan
 *
 * All endpoints require a valid JWT Bearer token.
 *
 * ┌────────────────────────────────────────────────────────────────────────────┐
 * │  Method  │  URL                             │  Purpose                    │
 * ├──────────┼──────────────────────────────────┼─────────────────────────────┤
 * │  POST    │  /api/meal/generate              │  UC006 Generate new plan    │
 * │  GET     │  /api/meal/plans                 │  UC007 List all saved plans │
 * │  GET     │  /api/meal/plans/{id}            │  UC007 Get a specific plan  │
 * │  PUT     │  /api/meal/plans/{id}            │  UC007 Edit plan content    │
 * │  PUT     │  /api/meal/plans/{id}/activate   │  UC007 Set plan as active   │
 * │  PATCH   │  /api/meal/plans/{id}/progress   │  UC007 Update progress      │
 * │  DELETE  │  /api/meal/plans/{id}            │  UC007 Delete a plan        │
 * └────────────────────────────────────────────────────────────────────────────┘
 */
@Slf4j
@RestController
@RequestMapping("/api/meal")
@RequiredArgsConstructor
public class MealPlanController {

    private final MealPlanService mealPlanService;

    // ── UC006: Generate ───────────────────────────────────────────────────────

    /**
     * POST /api/meal/generate
     *
     * 201 Created → MealPlanResponse with the AI-generated 7-day plan
     * 400         → @Valid field errors
     * 503         → Ollama not running / model not pulled
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<MealPlanResponse>> generatePlan(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MealPlanRequest request) {

        log.debug("UC006 Generate meal plan — user: {}", userDetails.getUsername());

        try {
            MealPlanResponse response =
                    mealPlanService.generatePlan(userDetails.getUsername(), request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Meal plan generated successfully!", response));

        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("AI engine is not available")) {
                return ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(ApiResponse.error(e.getMessage()));
            }
            throw e;
        }
    }

    // ── UC007: List plans ─────────────────────────────────────────────────────

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<MealPlanResponse>>> getPlans(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<MealPlanResponse> plans =
                mealPlanService.getPlans(userDetails.getUsername());

        return ResponseEntity.ok(ApiResponse.success("Plans loaded successfully.", plans));
    }

    // ── UC007: Get single plan ────────────────────────────────────────────────

    @GetMapping("/plans/{id}")
    public ResponseEntity<ApiResponse<MealPlanResponse>> getPlan(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id) {

        MealPlanResponse plan = mealPlanService.getPlan(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Plan loaded.", plan));
    }

    // ── UC007: Edit plan content ──────────────────────────────────────────────

    /**
     * PUT /api/meal/plans/{id}
     *
     * Allows the athlete to rename the plan and/or edit the generated markdown.
     * 200 OK → updated MealPlanResponse
     */
    @PutMapping("/plans/{id}")
    public ResponseEntity<ApiResponse<MealPlanResponse>> editPlan(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id,
            @Valid @RequestBody PlanEditRequest request) {

        MealPlanResponse plan =
                mealPlanService.editPlan(userDetails.getUsername(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Meal plan updated successfully.", plan));
    }

    // ── UC007: Activate plan ──────────────────────────────────────────────────

    /**
     * PUT /api/meal/plans/{id}/activate
     *
     * Sets the given plan as the user's currently active meal plan.
     * All other plans for this user are deactivated.
     * 200 OK → updated MealPlanResponse
     */
    @PutMapping("/plans/{id}/activate")
    public ResponseEntity<ApiResponse<MealPlanResponse>> activatePlan(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id) {

        MealPlanResponse plan =
                mealPlanService.activatePlan(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Plan set as currently active.", plan));
    }

    // ── UC006 AF2: Copy a plan ────────────────────────────────────────────────

    /**
     * POST /api/meal/plans/{id}/copy
     *
     * Duplicates the given plan with a new name. The copy is inactive and
     * has an empty progress list.
     * 201 Created → new MealPlanResponse
     */
    @PostMapping("/plans/{id}/copy")
    public ResponseEntity<ApiResponse<MealPlanResponse>> copyPlan(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id,
            @Valid @RequestBody CopyPlanRequest request) {

        MealPlanResponse plan =
                mealPlanService.copyPlan(userDetails.getUsername(), id, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Meal plan copied successfully.", plan));
    }

    // ── UC007: Update progress ────────────────────────────────────────────────

    /**
     * PATCH /api/meal/plans/{id}/progress
     *
     * Replaces the completedItems list for a plan.
     * 200 OK → updated MealPlanResponse
     */
    @PatchMapping("/plans/{id}/progress")
    public ResponseEntity<ApiResponse<MealPlanResponse>> updateProgress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id,
            @RequestBody PlanProgressRequest request) {

        MealPlanResponse plan =
                mealPlanService.updateProgress(userDetails.getUsername(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Progress updated.", plan));
    }

    // ── UC007: Delete plan ────────────────────────────────────────────────────

    @DeleteMapping("/plans/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePlan(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id) {

        mealPlanService.deletePlan(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Meal plan deleted successfully.", null));
    }
}
