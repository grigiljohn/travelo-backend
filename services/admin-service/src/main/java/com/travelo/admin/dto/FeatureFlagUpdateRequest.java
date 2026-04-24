package com.travelo.admin.dto;

import com.travelo.admin.domain.FeaturePlatform;

/**
 * All fields optional (partial update).
 */
public record FeatureFlagUpdateRequest(
        Boolean isEnabled,
        Integer rolloutPercentage,
        FeaturePlatform platform
) {
}
