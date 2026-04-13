package com.travelo.momentsservice.dto;

/**
 * A comment on a moment (lightweight social thread).
 */
public record MomentCommentResponse(
        String id,
        String momentId,
        String userId,
        String username,
        String commentText,
        String createdAt
) {
}
