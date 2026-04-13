package com.travelo.planservice.dto;

public record PlanStepResponse(
        String id,
        int stepOrder,
        String title,
        String time
) {
}
