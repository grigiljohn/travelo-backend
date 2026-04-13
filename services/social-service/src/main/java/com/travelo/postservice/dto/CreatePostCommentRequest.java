package com.travelo.postservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for creating a post comment.
 */
public record CreatePostCommentRequest(
    @NotBlank(message = "Comment text is required")
    @Size(max = 2000, message = "Comment text must not exceed 2000 characters")
    String commentText,
    
    UUID parentId // For threaded replies
) {}

