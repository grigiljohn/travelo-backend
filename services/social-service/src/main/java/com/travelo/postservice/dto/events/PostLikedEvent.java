package com.travelo.postservice.dto.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Emitted on `post.liked` (action="liked") or `post.like.removed` (action="unliked")
 * Kafka topic after a like is toggled.
 * Consumers: realtime-service (notifications), analytics pipeline.
 */
public record PostLikedEvent(
        @JsonProperty("post_id")        String postId,
        @JsonProperty("post_owner_id")  String postOwnerId,
        @JsonProperty("actor_user_id")  String actorUserId,
        @JsonProperty("action")         String action,
        @JsonProperty("total_likes")    long totalLikes,
        @JsonProperty("occurred_at")    Instant occurredAt
) {
    public static PostLikedEvent liked(String postId, String ownerId, String actorId, long total) {
        return new PostLikedEvent(postId, ownerId, actorId, "liked", total, Instant.now());
    }

    public static PostLikedEvent unliked(String postId, String ownerId, String actorId, long total) {
        return new PostLikedEvent(postId, ownerId, actorId, "unliked", total, Instant.now());
    }
}
