package com.travelo.storyservice.controller;

import com.travelo.commons.security.SecurityUtils;
import com.travelo.storyservice.dto.*;
import com.travelo.storyservice.service.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for story endpoints.
 */
@RestController
@RequestMapping("/api/v1/stories")
@Tag(name = "Stories", description = "Story management APIs")
public class StoryController {

    private static final Logger logger = LoggerFactory.getLogger(StoryController.class);

    private final StoryService storyService;

    public StoryController(StoryService storyService) {
        this.storyService = storyService;
    }

    @PostMapping
    @Operation(summary = "Create story", description = "Create a new story with media")
    public ResponseEntity<StoryDto> createStory(
            @Parameter(description = "User ID (optional when JWT is present)")
            @RequestParam(value = "userId", required = false) String userIdParam,
            @RequestBody CreateStoryRequest request) {
        String userId = (userIdParam != null && !userIdParam.isBlank())
                ? userIdParam
                : SecurityUtils.getCurrentUserIdAsString();
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        logger.info("POST /api/v1/stories - userId={}", userId);
        StoryDto story = storyService.createStory(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(story);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user stories", description = "Get active stories for a user")
    public ResponseEntity<List<StoryDto>> getUserStories(
            @Parameter(description = "User ID") @PathVariable("userId") String userId,
            @Parameter(description = "Viewer user ID (optional)") @RequestParam(value = "viewerUserId", required = false) String viewerUserId) {
        logger.info("GET /api/v1/stories/user/{}", userId);
        List<StoryDto> stories = storyService.getUserStories(userId, viewerUserId);
        return ResponseEntity.ok(stories);
    }

    @GetMapping("/feed")
    @Operation(summary = "Get feed stories", description = "Get active stories from followed users")
    public ResponseEntity<List<StoryDto>> getFeedStories(
            @Parameter(description = "List of followed user IDs", required = true)
            @RequestParam("followedUserIds") List<String> followedUserIds,
            @Parameter(description = "Viewer user ID") @RequestParam(value = "viewerUserId", required = false) String viewerUserId) {
        logger.info("GET /api/v1/stories/feed - {} followed users", followedUserIds.size());
        List<StoryDto> stories = storyService.getFeedStories(followedUserIds, viewerUserId);
        return ResponseEntity.ok(stories);
    }

    @GetMapping("/discover")
    @Operation(summary = "Get discover stories", description = "Get active stories for stories strip")
    public ResponseEntity<List<StoryDto>> getDiscoverStories(
            @Parameter(description = "Viewer user ID (optional)") @RequestParam(value = "viewerUserId", required = false) String viewerUserId) {
        logger.info("GET /api/v1/stories/discover");
        List<StoryDto> stories = storyService.getDiscoverStories(viewerUserId);
        return ResponseEntity.ok(stories);
    }

    @GetMapping("/{storyId}")
    @Operation(summary = "Get story", description = "Get a single story by ID")
    public ResponseEntity<StoryDto> getStory(
            @Parameter(description = "Story ID") @PathVariable("storyId") UUID storyId,
            @Parameter(description = "Viewer user ID (optional)") @RequestParam(value = "viewerUserId", required = false) String viewerUserId) {
        logger.info("GET /api/v1/stories/{}", storyId);
        StoryDto story = storyService.getStory(storyId, viewerUserId);
        return ResponseEntity.ok(story);
    }

    @DeleteMapping("/{storyId}")
    @Operation(summary = "Delete story", description = "Delete a story")
    public ResponseEntity<Void> deleteStory(
            @Parameter(description = "Story ID") @PathVariable("storyId") UUID storyId,
            @Parameter(description = "User ID", required = true) @RequestParam("userId") String userId) {
        logger.info("DELETE /api/v1/stories/{} - userId={}", storyId, userId);
        storyService.deleteStory(storyId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{storyId}/view")
    @Operation(summary = "Mark story as viewed", description = "Mark a story as viewed by a user")
    public ResponseEntity<Void> markStoryAsViewed(
            @Parameter(description = "Story ID") @PathVariable("storyId") UUID storyId,
            @Parameter(description = "User ID", required = true) @RequestParam("userId") String userId) {
        logger.info("POST /api/v1/stories/{}/view - userId={}", storyId, userId);
        storyService.markStoryAsViewed(storyId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{storyId}/viewers")
    @Operation(summary = "Get story viewers", description = "Get list of users who viewed a story")
    public ResponseEntity<List<StoryViewerDto>> getStoryViewers(
            @Parameter(description = "Story ID") @PathVariable("storyId") UUID storyId,
            @Parameter(description = "Owner user ID", required = true) @RequestParam("ownerUserId") String ownerUserId) {
        logger.info("GET /api/v1/stories/{}/viewers", storyId);
        List<StoryViewerDto> viewers = storyService.getStoryViewers(storyId, ownerUserId);
        return ResponseEntity.ok(viewers);
    }

    @PostMapping("/{storyId}/replies")
    @Operation(summary = "Add reply", description = "Add a reply to a story")
    public ResponseEntity<StoryReplyDto> addReply(
            @Parameter(description = "Story ID") @PathVariable("storyId") UUID storyId,
            @Parameter(description = "User ID", required = true) @RequestParam("userId") String userId,
            @RequestBody CreateStoryReplyRequest request) {
        logger.info("POST /api/v1/stories/{}/replies - userId={}", storyId, userId);
        StoryReplyDto reply = storyService.addReply(storyId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reply);
    }

    @GetMapping("/{storyId}/replies")
    @Operation(summary = "Get story replies", description = "Get all replies for a story")
    public ResponseEntity<List<StoryReplyDto>> getStoryReplies(
            @Parameter(description = "Story ID") @PathVariable("storyId") UUID storyId,
            @Parameter(description = "Viewer user ID (optional)") @RequestParam(value = "viewerUserId", required = false) String viewerUserId) {
        logger.info("GET /api/v1/stories/{}/replies", storyId);
        List<StoryReplyDto> replies = storyService.getStoryReplies(storyId, viewerUserId);
        return ResponseEntity.ok(replies);
    }
}

