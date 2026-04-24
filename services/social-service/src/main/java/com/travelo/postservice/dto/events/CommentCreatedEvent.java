package com.travelo.postservice.dto.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted on `comment.created` Kafka topic after a new comment is persisted.
 * Consumers: realtime-service (notifications), analytics pipeline.
 */
public record CommentCreatedEvent(
        @JsonProperty("comment_id")        UUID commentId,
        @JsonProperty("post_id")           String postId,
        @JsonProperty("post_owner_id")     String postOwnerId,
        @JsonProperty("actor_user_id")     String actorUserId,
        @JsonProperty("parent_comment_id") UUID parentCommentId,
        @JsonProperty("preview")           String preview,
        @JsonProperty("created_at")        Instant createdAt
) {
    private static final int PREVIEW_MAX = 140;

    public static CommentCreatedEvent of(UUID id, String postId, String ownerId, String actorId,
                                         UUID parentId, String text) {
        String preview = text == null ? "" :
                (text.length() <= PREVIEW_MAX ? text : text.substring(0, PREVIEW_MAX) + "…");
        return new CommentCreatedEvent(id, postId, ownerId, actorId, parentId, preview, Instant.now());
    }
}
