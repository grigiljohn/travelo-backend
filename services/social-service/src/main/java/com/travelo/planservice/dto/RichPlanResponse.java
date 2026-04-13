package com.travelo.planservice.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full plan for feed, detail, and preview (includes engagement fields for ranking).
 */
public record RichPlanResponse(
        String id,
        String userId,
        String title,
        String description,
        String locationName,
        Double lat,
        Double lng,
        String dateTimeIso,
        String timeLabel,
        int maxPeople,
        String audience,
        String skillLevel,
        List<String> tags,
        String mediaUrl,
        String mediaType,
        String badge,
        String visibility,
        boolean paid,
        BigDecimal pricePerPerson,
        List<PlanStepResponse> steps,
        String hostName,
        String hostAvatarUrl,
        double engagementScore,
        int viewsCount,
        int likesCount,
        String createdAtIso
) {
}
