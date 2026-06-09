package com.utm.rugbyplanner.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * CopyPlanRequest — payload for UC004 AF2 / UC006 AF2: Copy existing plan.
 *
 * The client supplies only the new plan name; all other fields are duplicated
 * from the source plan by the service layer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CopyPlanRequest {

    @NotBlank(message = "New plan name is required.")
    private String newName;
}
