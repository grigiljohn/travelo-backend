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
        return fromEntity(comment, null, false);
    }

    public static PostCommentDto fromEntity(PostComment comment, List<PostCommentDto> replies, boolean isLiked) {
        // TODO: Fetch username and avatarUrl from user-service
        // For now, use mock data based on userId
        String username = getMockUsername(comment.getUserId());
        String avatarUrl = getMockAvatarUrl(comment.getUserId());
        
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
    
    /**
     * Get mock username based on userId.
     * TODO: Replace with actual user-service call
     */
    private static String getMockUsername(String userId) {
        // Map common mock user IDs to usernames
        return switch (userId) {
            case "user-1" -> "jane_doe";
            case "user-2" -> "travel_lover";
            case "user-3" -> "photography_enthusiast";
            default -> "user_" + userId.substring(0, Math.min(8, userId.length()));
        };
    }
    
    /**
     * Get mock avatar URL based on userId.
     * TODO: Replace with actual user-service call
     */
    private static String getMockAvatarUrl(String userId) {
        // Map common mock user IDs to avatar URLs
        int avatarIndex = switch (userId) {
            case "user-1" -> 1;
            case "user-2" -> 2;
            case "user-3" -> 3;
            default -> Math.abs(userId.hashCode() % 10) + 1;
        };
        return "https://i.pravatar.cc/150?img=" + avatarIndex;
    }
}

