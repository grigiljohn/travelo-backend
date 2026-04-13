package com.travelo.reelservice.controller;

import com.travelo.commons.security.SecurityUtils;
import com.travelo.reelservice.dto.*;
import com.travelo.reelservice.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for reel endpoints.
 * Handles reel ingestion, feed, likes, comments, and views.
 */
@RestController
@RequestMapping("/api/v1/reels")
@Tag(name = "Reels", description = "Reel APIs with ingestion, ranking, likes, and comments")
public class ReelController {

    private static final Logger logger = LoggerFactory.getLogger(ReelController.class);

    private final ReelService reelService;
    private final ReelIngestionService reelIngestionService;
    private final ReelLikeService reelLikeService;
    private final ReelCommentService reelCommentService;

    public ReelController(ReelService reelService,
                         ReelIngestionService reelIngestionService,
                         ReelLikeService reelLikeService,
                         ReelCommentService reelCommentService) {
        this.reelService = reelService;
        this.reelIngestionService = reelIngestionService;
        this.reelLikeService = reelLikeService;
        this.reelCommentService = reelCommentService;
    }

    @PostMapping("/ingest")
    @Operation(summary = "Ingest reel", description = "Create a new reel from uploaded video")
    public ResponseEntity<ReelDto> ingestReel(
            @Parameter(description = "User ID (optional when JWT is present)")
            @RequestParam(value = "userId", required = false) String userIdParam,
            @RequestBody CreateReelRequest request) {
        String userId = (userIdParam != null && !userIdParam.isBlank())
                ? userIdParam
                : SecurityUtils.getCurrentUserIdAsString();
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        logger.info("POST /api/v1/reels/ingest - userId={}", userId);
        ReelDto reel = reelIngestionService.createReel(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reel);
    }

    @GetMapping
    @Operation(summary = "Get reels", 
               description = "Get personalized ranked reel feed with reels and ads merged together")
    public ResponseEntity<Map<String, Object>> getReels(
            @Parameter(description = "User ID", required = true)
            @RequestParam UUID userId,
            @Parameter(description = "Page number", example = "1")
            @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @Parameter(description = "Mood filter", required = false)
            @RequestParam(value = "mood", required = false) String mood) {
        
        logger.info("GET /api/v1/reels - userId={}, page={}, limit={}, mood={}", userId, page, limit, mood);
        
        try {
            List<ReelItem> reels = reelService.getReels(userId, page, limit, mood);
            
            // Wrap response in expected format
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reels);
            response.put("message", "Reels fetched successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in getReels endpoint for userId={}, page={}, limit={}: {}", 
                    userId, page, limit, e.getMessage(), e);
            // Exception will be handled by GlobalExceptionHandler
            throw e;
        }
    }

    @PostMapping("/{reelId}/like")
    @Operation(summary = "Like reel", description = "Like a reel")
    public ResponseEntity<Void> likeReel(
            @Parameter(description = "Reel ID") @PathVariable("reelId") UUID reelId,
            @Parameter(description = "User ID", required = true) @RequestParam String userId) {
        logger.info("POST /api/v1/reels/{}/like - userId={}", reelId, userId);
        reelLikeService.likeReel(reelId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{reelId}/like")
    @Operation(summary = "Unlike reel", description = "Unlike a reel")
    public ResponseEntity<Void> unlikeReel(
            @Parameter(description = "Reel ID") @PathVariable("reelId") UUID reelId,
            @Parameter(description = "User ID", required = true) @RequestParam String userId) {
        logger.info("DELETE /api/v1/reels/{}/like - userId={}", reelId, userId);
        reelLikeService.unlikeReel(reelId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{reelId}/comments")
    @Operation(summary = "Add comment", description = "Add a comment to a reel")
    public ResponseEntity<ReelCommentDto> addComment(
            @Parameter(description = "Reel ID") @PathVariable("reelId") UUID reelId,
            @Parameter(description = "User ID", required = true) @RequestParam String userId,
            @RequestBody CreateReelCommentRequest request) {
        logger.info("POST /api/v1/reels/{}/comments - userId={}", reelId, userId);
        ReelCommentDto comment = reelCommentService.addComment(reelId, userId, request.getCommentText(), request.getParentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @GetMapping("/{reelId}/comments")
    @Operation(summary = "Get comments", description = "Get comments for a reel")
    public ResponseEntity<Page<ReelCommentDto>> getComments(
            @Parameter(description = "Reel ID") @PathVariable("reelId") UUID reelId,
            @Parameter(description = "Page number") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Page size") @RequestParam(value = "limit", defaultValue = "10") int limit) {
        logger.info("GET /api/v1/reels/{}/comments", reelId);
        Page<ReelCommentDto> comments = reelCommentService.getComments(reelId, page, limit);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/{reelId}/view")
    @Operation(summary = "Track view", description = "Track a reel view with analytics")
    public ResponseEntity<Void> trackView(
            @Parameter(description = "Reel ID") @PathVariable("reelId") UUID reelId,
            @Parameter(description = "User ID", required = true) @RequestParam String userId,
            @RequestBody TrackReelViewRequest request) {
        logger.info("POST /api/v1/reels/{}/view - userId={}", reelId, userId);
        // TODO: Implement view tracking service
        return ResponseEntity.ok().build();
    }
}

