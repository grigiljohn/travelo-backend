package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One user who liked a post — returned by {@code GET /api/v1/posts/{postId}/likes}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostLikeUserDto(
        String userId,
        String username,
        String displayName,
        String avatarUrl
) {
}
