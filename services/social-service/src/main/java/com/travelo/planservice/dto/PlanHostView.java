package com.travelo.planservice.dto;

/**
 * Host block for plan detail (trust / conversion).
 */
public record PlanHostView(
        String id,
        String name,
        String avatar,
        double rating,
        boolean verified
) {
}
