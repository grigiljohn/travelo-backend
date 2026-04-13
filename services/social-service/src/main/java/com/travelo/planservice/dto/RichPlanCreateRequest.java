package com.travelo.planservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Next-gen plan create payload (maps to {@code plans} + {@code plan_steps} conceptually).
 */
public record RichPlanCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        @NotBlank @Size(max = 300) String locationName,
        Double lat,
        Double lng,
        @NotBlank String dateTimeIso,
        @Min(2) @Max(200) int maxPeople,
        @NotBlank @Size(max = 40) String audience,
        @NotBlank @Size(max = 40) String skillLevel,
        @NotNull List<@Size(max = 48) String> tags,
        @NotBlank @Size(max = 2000) String mediaUrl,
        @NotBlank @Size(max = 16) String mediaType,
        String badge,
        @NotBlank @Size(max = 24) String visibility,
        boolean paid,
        BigDecimal pricePerPerson,
        @Valid List<PlanStepInput> steps,
        @Size(max = 120) String hostName,
        @Size(max = 2000) String hostAvatarUrl
) {
    public RichPlanCreateRequest {
        if (description == null) {
            description = "";
        }
        if (badge == null || badge.isBlank()) {
            badge = "NONE";
        }
        if (steps == null) {
            steps = List.of();
        }
        if (tags == null) {
            tags = List.of();
        }
        if (hostName == null) {
            hostName = "";
        }
        if (hostAvatarUrl == null) {
            hostAvatarUrl = "";
        }
    }
}
