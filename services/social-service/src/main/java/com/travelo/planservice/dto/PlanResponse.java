package com.travelo.planservice.dto;

import java.util.List;

/**
 * Plan returned from {@code GET /api/v1/plans} and {@code POST /api/v1/plans}.
 */
public record PlanResponse(
        String id,
        String title,
        String location,
        String timeLabel,
        String hostUserId,
        String hostName,
        String hostAvatarUrl,
        List<String> participantAvatarUrls,
        int joined,
        int maxPeople,
        String badge,
        String heroImageUrl,
        String description,
        /** Hosting community id when the event is tied to a circle (empty string when none). */
        String organizerCommunityId
) {
}
