package com.travelo.postservice.controller;

import com.travelo.commons.security.SecurityUtils;
import com.travelo.postservice.dto.PostCommentDto;
import com.travelo.postservice.dto.CreatePostCommentRequest;
import com.travelo.postservice.dto.ApiResponse;
import com.travelo.postservice.dto.PageResponse;
import com.travelo.postservice.service.PostCommentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
public class PostCommentController {

    private static final Logger logger = LoggerFactory.getLogger(PostCommentController.class);
    private final PostCommentService postCommentService;

    public PostCommentController(PostCommentService postCommentService) {
        this.postCommentService = postCommentService;
        logger.info("PostCommentController initialized");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<PostCommentDto>> addComment(
            @PathVariable String postId,
            @Valid @RequestBody CreatePostCommentRequest request) {
        // Extract user ID from JWT token (P0 security fix - remove insecure X-User-Id header)
        String userId = SecurityUtils.getCurrentUserIdAsString();
        if (userId == null) {
            logger.warn("Unauthenticated attempt to add comment to post: {}", postId);
            throw new com.travelo.postservice.exception.UnauthorizedException("User not authenticated");
        }
        
        logger.info("Adding comment to post {} by user {}", postId, userId);
        try {
            PostCommentDto comment = postCommentService.addComment(postId, userId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Comment added successfully", comment));
        } catch (Exception e) {
            logger.error("Error adding comment to post {}", postId, e);
            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PostCommentDto>>> getComments(
            @PathVariable String postId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        // Extract user ID from JWT token if authenticated (optional for viewing comments)
        String currentUserId = SecurityUtils.getCurrentUserIdAsString();
        logger.debug("Getting comments for post {} - page: {}, limit: {}", postId, page, limit);
        try {
            Page<PostCommentDto> comments = postCommentService.getComments(postId, page, limit, currentUserId);
            PageResponse<PostCommentDto> pageResponse = PageResponse.of(comments);
            return ResponseEntity.ok(ApiResponse.success("Comments retrieved successfully", pageResponse));
        } catch (Exception e) {
            logger.error("Error retrieving comments for post {}", postId, e);
            throw e;
        }
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<ApiResponse<PostCommentDto>> getComment(
            @PathVariable String postId,
            @PathVariable("commentId") UUID commentId) {
        // Extract user ID from JWT token if authenticated (optional for viewing)
        String currentUserId = SecurityUtils.getCurrentUserIdAsString();
        logger.debug("Getting comment {} for post {}", commentId, postId);
        try {
            PostCommentDto comment = postCommentService.getCommentById(commentId, currentUserId);
            return ResponseEntity.ok(ApiResponse.success("Comment retrieved successfully", comment));
        } catch (Exception e) {
            logger.error("Error retrieving comment {}", commentId, e);
            throw e;
        }
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<PostCommentDto>> updateComment(
            @PathVariable String postId,
            @PathVariable("commentId") UUID commentId,
            @RequestBody UpdateCommentRequest request) {
        // Extract user ID from JWT token (P0 security fix - remove insecure X-User-Id header)
        String userId = SecurityUtils.getCurrentUserIdAsString();
        if (userId == null) {
            logger.warn("Unauthenticated attempt to update comment: {}", commentId);
            throw new com.travelo.postservice.exception.UnauthorizedException("User not authenticated");
        }
        
        logger.info("Updating comment {} by user {}", commentId, userId);
        try {
            PostCommentDto comment = postCommentService.updateComment(commentId, userId, request.commentText());
            return ResponseEntity.ok(ApiResponse.success("Comment updated successfully", comment));
        } catch (Exception e) {
            logger.error("Error updating comment {}", commentId, e);
            throw e;
        }
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable String postId,
            @PathVariable("commentId") UUID commentId) {
        // Extract user ID from JWT token (P0 security fix - remove insecure X-User-Id header)
        String userId = SecurityUtils.getCurrentUserIdAsString();
        if (userId == null) {
            logger.warn("Unauthenticated attempt to delete comment: {}", commentId);
            throw new com.travelo.postservice.exception.UnauthorizedException("User not authenticated");
        }
        
        logger.info("Deleting comment {} by user {}", commentId, userId);
        try {
            postCommentService.deleteComment(commentId, userId);
            return ResponseEntity.ok(ApiResponse.success("Comment deleted successfully", null));
        } catch (Exception e) {
            logger.error("Error deleting comment {}", commentId, e);
            throw e;
        }
    }

    @PostMapping("/{commentId}/like")
    public ResponseEntity<ApiResponse<PostCommentDto>> likeComment(
            @PathVariable String postId,
            @PathVariable("commentId") UUID commentId,
            @RequestBody(required = false) LikeCommentRequest request) {
        // Extract user ID from JWT token (P0 security fix - remove insecure X-User-Id header)
        String userId = SecurityUtils.getCurrentUserIdAsString();
        if (userId == null) {
            logger.warn("Unauthenticated attempt to like comment: {}", commentId);
            throw new com.travelo.postservice.exception.UnauthorizedException("User not authenticated");
        }
        
        boolean liked = request != null && request.liked() != null ? request.liked() : true;
        logger.info("{} comment {} by user {}", liked ? "Liking" : "Unliking", commentId, userId);
        try {
            PostCommentDto comment = postCommentService.likeComment(commentId, userId, liked);
            String message = liked ? "Comment liked successfully" : "Comment unliked successfully";
            return ResponseEntity.ok(ApiResponse.success(message, comment));
        } catch (Exception e) {
            logger.error("Error {} comment {}", liked ? "liking" : "unliking", commentId, e);
            throw e;
        }
    }

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<ApiResponse<PageResponse<PostCommentDto>>> getCommentReplies(
            @PathVariable String postId,
            @PathVariable("commentId") UUID commentId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        // Extract user ID from JWT token if authenticated (optional for viewing)
        String currentUserId = SecurityUtils.getCurrentUserIdAsString();
        logger.debug("Getting replies for comment {} - page: {}, limit: {}", commentId, page, limit);
        try {
            Page<PostCommentDto> replies = postCommentService.getCommentReplies(commentId, page, limit, currentUserId);
            PageResponse<PostCommentDto> pageResponse = PageResponse.of(replies);
            return ResponseEntity.ok(ApiResponse.success("Replies retrieved successfully", pageResponse));
        } catch (Exception e) {
            logger.error("Error retrieving replies for comment {}", commentId, e);
            throw e;
        }
    }

    // Inner classes for requests
    public record UpdateCommentRequest(String commentText) {}
    public record LikeCommentRequest(Boolean liked) {}
}

