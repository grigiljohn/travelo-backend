package com.travelo.momentsservice.dto;

import java.time.OffsetDateTime;

public record MomentFeedItemResponse(
        String id,
        String userId,
        String userName,
        String caption,
        String location,
        String type,
        String mediaType,
        String thumbnailUrl,
        String musicName,
        boolean aiEnhanced,
        String mediaUrl,
        OffsetDateTime createdAt,
        int likeCount,
        int commentCount,
        boolean isLiked,
        boolean isViewed
) {
}
