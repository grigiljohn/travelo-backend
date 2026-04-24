package com.travelo.userservice.dto.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Emitted on {@code user.followed} (action="followed") or {@code user.unfollowed}
 * (action="unfollowed") Kafka topic after a follow relationship is toggled.
 *
 * <p>Consumers:
 * <ul>
 *   <li>{@code social-service} — refresh follower's feed on followed,
 *       drop followee's posts on unfollowed.</li>
 *   <li>{@code realtime-service} — create a {@code USER_FOLLOWED}
 *       notification for the followee.</li>
 * </ul>
 */
public record UserFollowedEvent(
        @JsonProperty("follower_id")  String followerId,
        @JsonProperty("followee_id")  String followeeId,
        @JsonProperty("action")       String action,
        @JsonProperty("occurred_at")  Instant occurredAt
) {
    public static UserFollowedEvent followed(String followerId, String followeeId) {
        return new UserFollowedEvent(followerId, followeeId, "followed", Instant.now());
    }

    public static UserFollowedEvent unfollowed(String followerId, String followeeId) {
        return new UserFollowedEvent(followerId, followeeId, "unfollowed", Instant.now());
    }
}
