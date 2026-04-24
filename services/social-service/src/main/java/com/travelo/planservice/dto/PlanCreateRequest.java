package com.travelo.planservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/plans}.
 */
public record PlanCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 200) String location,
        @NotBlank @Size(max = 200) String timeLabel,
        @Min(2) @Max(500) int maxPeople,
        @Size(max = 2000) String description,
        @Size(max = 2000) String heroImageUrl,
        /** One of: {@code TRENDING}, {@code HAPPENING_NOW}, {@code NONE} (default). */
        String badge,
        /** Optional display override when JWT has no name claim. */
        @Size(max = 120) String hostName,
        @Size(max = 2000) String hostAvatarUrl,
        @Size(max = 64) String organizerCommunityId,
        @Size(max = 128) String externalPlaceId,
        Double latitude,
        Double longitude,
        /** ISO-8601 instant for sorting / calendar (optional while clients send only timeLabel). */
        String startsAtIso,
        Boolean requireApprovalToJoin,
        Boolean allowWaitlist,
        @Size(max = 32) String privacy
) {
    public PlanCreateRequest {
        if (description == null) {
            description = "";
        }
        if (heroImageUrl == null) {
            heroImageUrl = "";
        }
        if (badge == null || badge.isBlank()) {
            badge = "NONE";
        }
        if (hostName == null) {
            hostName = "";
        }
        if (hostAvatarUrl == null) {
            hostAvatarUrl = "";
        }
        if (organizerCommunityId == null) {
            organizerCommunityId = "";
        }
        if (externalPlaceId == null) {
            externalPlaceId = "";
        }
        if (requireApprovalToJoin == null) {
            requireApprovalToJoin = Boolean.FALSE;
        }
        if (allowWaitlist == null) {
            allowWaitlist = Boolean.TRUE;
        }
        if (privacy == null || privacy.isBlank()) {
            privacy = "PUBLIC";
        }
        if (startsAtIso == null) {
            startsAtIso = "";
        }
    }
}
