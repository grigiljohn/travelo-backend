package com.travelo.postservice.realtime;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Wire DTO pushed over SSE to comment-stream subscribers. Lean on purpose:
 * the client already knows which post it's subscribed to, so only fields
 * actually needed to render a new row are included.
 *
 * <p>Field names mirror the public REST shape of {@code PostCommentDto} so
 * the mobile client can reuse its existing JSON parser for comments.
 */
public record CommentStreamEvent(
        @JsonProperty("id") String id,
        @JsonProperty("postId") String postId,
        @JsonProperty("userId") String userId,
        @JsonProperty("commentText") String commentText,
        @JsonProperty("parentId") String parentId,
        @JsonProperty("username") String username,
        @JsonProperty("avatarUrl") String avatarUrl,
        @JsonProperty("createdAt") OffsetDateTime createdAt
) {
    public CommentStreamEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(postId, "postId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(commentText, "commentText");
    }
}
