package com.travelo.planservice.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Plan detail for mobile (nested host, engagement, chat preview, participants).
 */
public record RichPlanDetailResponse(
        String id,
        String title,
        String description,
        String mediaUrl,
        String mediaType,
        PlanHostView host,
        String dateTime,
        String locationName,
        Double lat,
        Double lng,
        BigDecimal price,
        boolean paid,
        int maxPeople,
        int participantsCount,
        List<PlanParticipantPreview> participants,
        List<PlanChatPreviewMessage> chatPreview,
        PlanEngagementView engagement,
        String timeLabel,
        List<PlanStepResponse> steps,
        String badge,
        String audience,
        String skillLevel,
        List<String> tags,
        boolean viewerJoined
) {
}
