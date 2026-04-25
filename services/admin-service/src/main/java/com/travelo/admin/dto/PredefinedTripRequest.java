package com.travelo.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record PredefinedTripRequest(
        @NotBlank @Size(max = 120) String slug,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 500) String subtitle,
        @Size(max = 2000) String heroImageUrl,
        int sortOrder,
        boolean active,
        Integer estimatedDays,
        @NotNull Map<String, Object> tripPreferences
) {
}
