package com.travelo.feedservice.controller;

import com.travelo.commons.security.SecurityUtils;
import com.travelo.feedservice.dto.FeedResponse;
import com.travelo.feedservice.dto.FeedRankingDebugResponse;
import com.travelo.feedservice.dto.FeedUserEventsRequest;
import com.travelo.feedservice.dto.MarkSeenRequest;
import com.travelo.feedservice.service.FeedInteractionEventPublisher;
import com.travelo.feedservice.service.FeedService;
import com.travelo.feedservice.service.FeedSeenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for feed endpoints.
 * Returns ranked feed with posts and ads merged together.
 * Supports cursor-based pagination.
 */
@RestController
@RequestMapping("/api/v1/feed")
@Tag(name = "Feed", description = "Feed APIs with ranking, caching, and ad integration")
public class FeedController {

    private static final Logger logger = LoggerFactory.getLogger(FeedController.class);

    private final FeedService feedService;
    private final FeedSeenService feedSeenService;
    private final FeedInteractionEventPublisher feedInteractionEventPublisher;
    private final boolean allowHeaderUserFallback;

    public FeedController(
            FeedService feedService,
            FeedSeenService feedSeenService,
            FeedInteractionEventPublisher feedInteractionEventPublisher,
            @Value("${app.dev.allow-header-user-fallback:false}") boolean allowHeaderUserFallback) {
        this.feedService = feedService;
        this.feedSeenService = feedSeenService;
        this.feedInteractionEventPublisher = feedInteractionEventPublisher;
        this.allowHeaderUserFallback = allowHeaderUserFallback;
    }

    @GetMapping
    @Operation(summary = "Get feed", 
               description = "Get personalized ranked feed with cursor-based pagination and seen filtering")
    public ResponseEntity<FeedResponse> getFeed(
            @Parameter(description = "User ID (extracted from JWT if not provided)", required = false)
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @Parameter(description = "Cursor for pagination (from next_cursor of previous response)", required = false)
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "Number of items to retrieve", example = "10")
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @Parameter(description = "Mood filter", required = false)
            @RequestParam(value = "mood", required = false) String mood,
            @Parameter(description = "Feed surface (e.g., 'home', 'explore', 'reels')", required = false)
            @RequestParam(value = "surface", defaultValue = "home") String surface) {
        
        // Extract userId from JWT if not provided (for security)
        UUID resolvedUserId = resolveUserId(userId, headerUserId, "getFeed");
        if (resolvedUserId == null) {
            logger.warn("No userId provided and user not authenticated");
            return ResponseEntity.badRequest().build();
        }
        
        logger.info("GET /api/v1/feed - userId={}, cursor={}, limit={}, mood={}, surface={}", 
                resolvedUserId, cursor, limit, mood, surface);
        
        FeedResponse feedResponse = feedService.getFeed(resolvedUserId, cursor, limit, mood, surface);
        
        return ResponseEntity.ok(feedResponse);
    }

    @GetMapping("/debug/ranking")
    @Operation(summary = "Explain ranking", description = "Debug endpoint showing ranking component scores and online signal effects.")
    public ResponseEntity<FeedRankingDebugResponse> debugRanking(
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "mood", required = false) String mood,
            @RequestParam(value = "surface", defaultValue = "home") String surface) {
        UUID resolvedUserId = userId != null ? userId : SecurityUtils.getCurrentUserId();
        resolvedUserId = resolveUserId(resolvedUserId, headerUserId, "debugRanking");
        if (resolvedUserId == null) {
            return ResponseEntity.badRequest().build();
        }
        FeedRankingDebugResponse response = feedService.debugRanking(resolvedUserId, limit, mood, surface);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/seen")
    @Operation(summary = "Mark posts as seen", 
               description = "Mark posts as seen for a user on a specific surface. Used for seen-once feed filtering.")
    public ResponseEntity<Void> markPostsAsSeen(
            @Parameter(description = "User ID (optional, for dev/local no-auth mode)", required = false)
            @RequestParam(value = "userId", required = false) UUID requestUserId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @Valid @RequestBody MarkSeenRequest request) {
        // Resolve in this order: explicit query param -> JWT -> dev header fallback.
        UUID userId = resolveUserId(requestUserId, headerUserId, "markPostsAsSeen");
        if (userId == null) {
            logger.warn("Unauthenticated attempt to mark posts as seen");
            return ResponseEntity.status(401).build();
        }
        
        if (request.getSurface() == null || request.getSurface().isEmpty()) {
            logger.warn("Missing surface in markPostsAsSeen request");
            return ResponseEntity.badRequest().build();
        }
        
        if (request.getPostIds() == null || request.getPostIds().isEmpty()) {
            logger.debug("Empty postIds in markPostsAsSeen request, ignoring");
            return ResponseEntity.ok().build();
        }
        
        logger.info("POST /api/v1/feed/seen - userId={}, surface={}, postCount={}", 
                userId, request.getSurface(), request.getPostIds().size());
        
        // Mark posts as seen asynchronously (non-blocking)
        feedSeenService.markPostsAsSeen(userId, request.getSurface(), request.getPostIds());
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/events")
    @Operation(summary = "Record feed interaction batch",
            description = "Impressions, clicks, dwell — logged and optionally emitted to Kafka for ML/analytics.")
    public ResponseEntity<Void> recordFeedEvents(
            @RequestParam(value = "userId", required = false) UUID requestUserId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @Valid @RequestBody FeedUserEventsRequest request) {
        UUID userId = resolveUserId(requestUserId, headerUserId, "recordFeedEvents");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        if (request.getEvents() == null || request.getEvents().isEmpty()) {
            return ResponseEntity.ok().build();
        }
        feedInteractionEventPublisher.publish(userId, request.getSurface(), request.getEvents());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh feed", 
               description = "Invalidate cache and trigger feed recomputation (internal use)")
    public ResponseEntity<Void> refreshFeed(
            @Parameter(description = "User ID", required = true)
            @RequestParam("userId") UUID userId) {
        
        logger.info("POST /api/v1/feed/refresh - userId={}", userId);
        
        feedService.refreshFeed(userId);
        
        return ResponseEntity.ok().build();
    }

    private UUID resolveUserId(UUID requestUserId, String headerUserId, String context) {
        UUID userId = requestUserId != null ? requestUserId : SecurityUtils.getCurrentUserId();
        if (userId == null && allowHeaderUserFallback && headerUserId != null && !headerUserId.isBlank()) {
            try {
                userId = UUID.fromString(headerUserId.trim());
            } catch (IllegalArgumentException ignored) {
                logger.warn("Invalid X-User-Id header for {}: {}", context, headerUserId);
            }
        }
        return userId;
    }
}

