package com.travelo.planservice.dto;

import jakarta.validation.constraints.Size;

/**
 * Itinerary line item for rich plan create.
 */
public record PlanStepInput(
        int stepOrder,
        @Size(max = 200) String title,
        @Size(max = 80) String time
) {
}
