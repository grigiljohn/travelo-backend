package com.travelo.admin.dto;

import com.travelo.admin.domain.FeaturePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EvaluateFeatureRequest(
        @NotBlank String featureName,
        @NotNull FeaturePlatform platform,
        String userId
) {
}
