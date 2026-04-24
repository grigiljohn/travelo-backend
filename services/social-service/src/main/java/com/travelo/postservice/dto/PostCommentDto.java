package com.travelo.postservice.dto;

import com.travelo.postservice.entity.PostComment;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for post comments.
 */
public record PostCommentDto(
    UUID id,
    String postId,
    String userId,
    String commentText,
    UUID parentId,
    Integer likeCount,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    OffsetDateTime createdAt,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    OffsetDateTime updatedAt,
    List<PostCommentDto> replies, // Nested replies
    Boolean isLiked, // Whether current user liked this comment (requires user context)
    String username, // Username of comment author (for display)
    String avatarUrl // Avatar URL of comment author (for display)
) {
    public static PostCommentDto fromEntity(PostComment comment) {
        return fromEntity(comment, null, false, null, null);
    }

    public static PostCommentDto fromEntity(PostComment comment, List<PostCommentDto> replies, boolean isLiked) {
        return fromEntity(comment, replies, isLiked, null, null);
    }

    /**
     * Build a DTO from a persisted comment, stamping the caller-resolved
     * {@code username}/{@code avatarUrl} when the user-service call succeeded.
     * Callers that lack identity context should pass nulls — the client will
     * then render a generic "User" affordance.
     */
    public static PostCommentDto fromEntity(PostComment comment,
                                            List<PostCommentDto> replies,
                                            boolean isLiked,
                                            String username,
                                            String avatarUrl) {
        return new PostCommentDto(
            comment.getId(),
            comment.getPostId(),
            comment.getUserId(),
            comment.getCommentText(),
            comment.getParentId(),
            comment.getLikeCount(),
            comment.getCreatedAt(),
            comment.getUpdatedAt(),
            replies != null ? replies : List.of(),
            isLiked,
            username,
            avatarUrl
        );
    }
}

