package com.travelo.postservice.service;

import com.travelo.postservice.dto.PostCommentDto;
import com.travelo.postservice.dto.CreatePostCommentRequest;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * Service for managing post comments.
 */
public interface PostCommentService {
    
    /**
     * Add a comment to a post.
     */
    PostCommentDto addComment(String postId, String userId, CreatePostCommentRequest request);
    
    /**
     * Get comments for a post (paginated, top-level only).
     */
    Page<PostCommentDto> getComments(String postId, int page, int limit, String currentUserId);
    
    /**
     * Get replies for a specific comment.
     */
    Page<PostCommentDto> getCommentReplies(UUID commentId, int page, int limit, String currentUserId);
    
    /**
     * Update a comment.
     */
    PostCommentDto updateComment(UUID commentId, String userId, String commentText);
    
    /**
     * Delete a comment (soft delete).
     */
    void deleteComment(UUID commentId, String userId);
    
    /**
     * Like or unlike a comment.
     */
    PostCommentDto likeComment(UUID commentId, String userId, boolean liked);
    
    /**
     * Get comment by ID.
     */
    PostCommentDto getCommentById(UUID commentId, String currentUserId);
}

