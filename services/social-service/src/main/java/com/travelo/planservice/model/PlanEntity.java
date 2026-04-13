package com.travelo.planservice.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class PlanEntity {
    private final String id;
    private final String title;
    private final String location;
    private final String timeLabel;
    private final String hostUserId;
    private final String hostName;
    private final String hostAvatarUrl;
    private final List<String> participantAvatarUrls;
    private final int joined;
    private final int maxPeople;
    private final String badge;
    private final String heroImageUrl;
    private final String description;
    private final Instant createdAt;

    public PlanEntity(
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
            Instant createdAt
    ) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.timeLabel = timeLabel;
        this.hostUserId = hostUserId;
        this.hostName = hostName;
        this.hostAvatarUrl = hostAvatarUrl;
        this.participantAvatarUrls = new ArrayList<>(participantAvatarUrls);
        this.joined = joined;
        this.maxPeople = maxPeople;
        this.badge = badge;
        this.heroImageUrl = heroImageUrl;
        this.description = description;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public com.travelo.planservice.dto.PlanResponse toResponse() {
        return new com.travelo.planservice.dto.PlanResponse(
                id,
                title,
                location,
                timeLabel,
                hostUserId,
                hostName,
                hostAvatarUrl,
                List.copyOf(participantAvatarUrls),
                joined,
                maxPeople,
                badge,
                heroImageUrl,
                description
        );
    }
}
