package com.travelo.postservice.controller;

import com.travelo.commons.security.SecurityUtils;
import com.travelo.postservice.dto.PostCommentDto;
import com.travelo.postservice.dto.CreatePostCommentRequest;
import com.travelo.postservice.dto.ApiResponse;
import com.travelo.postservice.dto.PageResponse;
import com.travelo.postservice.realtime.CommentStreamBroker;
import com.travelo.postservice.realtime.CommentStreamEvent;
import com.travelo.postservice.service.PostCommentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
public class PostCommentController {

    private static final Logger logger = LoggerFactory.getLogger(PostCommentController.class);
    /** Matches the reel-progress SSE stream — long enough to cover a realistic thread-watch session. */
    private static final long STREAM_IDLE_TIMEOUT_MS = 10L * 60L * 1000L; // 10 min
    /** Keep-alive tick so proxies (ALB, nginx) don't kill the connection while nobody comments. */
    private static final long STREAM_HEARTBEAT_MS = 25_000L;

    private final PostCommentService postCommentService;
    private final CommentStreamBroker commentStreamBroker;

    public PostCommentController(PostCommentService postCommentService,
                                 CommentStreamBroker commentStreamBroker) {
        this.postCommentService = postCommentService;
        this.commentStreamBroker = commentStreamBroker;
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

    /**
     * Server-Sent Events stream of new top-level + reply comments for {@code postId}.
     * One {@code comment} event per newly persisted row, carrying the same JSON
     * shape the client already parses from {@code GET /api/v1/posts/{postId}/comments}.
     *
     * <p>Stream closes after 10 minutes idle, when the client disconnects, or when
     * the server is shut down. Periodic SSE comment lines (<code>": hb"</code>)
     * keep the connection alive through LB idle timeouts.
     *
     * <p>GET /api/v1/posts/{postId}/comments/stream  (Accept: text/event-stream)
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamComments(@PathVariable String postId) {
        SseEmitter emitter = new SseEmitter(STREAM_IDLE_TIMEOUT_MS);

        AtomicBoolean closed = new AtomicBoolean(false);
        AtomicReference<CommentStreamBroker.Subscription> subRef = new AtomicReference<>();

        Runnable cleanup = () -> {
            if (closed.compareAndSet(false, true)) {
                CommentStreamBroker.Subscription s = subRef.getAndSet(null);
                if (s != null) {
                    try { s.close(); } catch (Exception ignored) { /* best-effort */ }
                }
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> { try { emitter.complete(); } catch (Exception ignored) {} cleanup.run(); });
        emitter.onError(e -> cleanup.run());

        Consumer<CommentStreamEvent> listener = ev -> {
            if (closed.get() || ev == null) return;
            try {
                emitter.send(SseEmitter.event().name("comment").data(ev));
            } catch (Exception e) {
                cleanup.run();
            }
        };

        subRef.set(commentStreamBroker.subscribe(postId, listener));

        // Initial "ready" ping so the client knows the subscription is live
        // even before the first comment arrives.
        try {
            emitter.send(SseEmitter.event().name("ready").data("ok"));
        } catch (Exception e) {
            cleanup.run();
            return emitter;
        }

        // Heartbeat thread — cheap & stateless, terminates when emitter closes.
        Thread heartbeat = new Thread(() -> {
            while (!closed.get()) {
                try {
                    Thread.sleep(STREAM_HEARTBEAT_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (closed.get()) return;
                try {
                    // A single-line SSE comment frame: valid, ignored by all clients, keeps proxies warm.
                    emitter.send(SseEmitter.event().comment("hb"));
                } catch (Exception e) {
                    cleanup.run();
                    return;
                }
            }
        }, "comments-sse-hb-" + postId);
        heartbeat.setDaemon(true);
        heartbeat.start();

        return emitter;
    }

    // Inner classes for requests
    public record UpdateCommentRequest(String commentText) {}
    public record LikeCommentRequest(Boolean liked) {}
}

