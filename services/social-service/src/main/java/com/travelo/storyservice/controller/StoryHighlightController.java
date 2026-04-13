package com.travelo.storyservice.controller;

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

import java.util.List;
import java.util.UUID;

/**
 * REST controller for story highlight endpoints.
 */
@RestController
@RequestMapping("/api/v1/highlights")
@Tag(name = "Story Highlights", description = "Story highlight management APIs")
public class StoryHighlightController {

    private static final Logger logger = LoggerFactory.getLogger(StoryHighlightController.class);

    private final StoryService storyService;

    public StoryHighlightController(StoryService storyService) {
        this.storyService = storyService;
    }

    @PostMapping
    @Operation(summary = "Create highlight", description = "Create a new story highlight")
    public ResponseEntity<StoryHighlightDto> createHighlight(
            @Parameter(description = "User ID", required = true) @RequestParam("userId") String userId,
            @RequestBody CreateStoryHighlightRequest request) {
        logger.info("POST /api/v1/highlights - userId={}", userId);
        StoryHighlightDto highlight = storyService.createHighlight(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(highlight);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user highlights", description = "Get all highlights for a user")
    public ResponseEntity<List<StoryHighlightDto>> getUserHighlights(
            @Parameter(description = "User ID") @PathVariable("userId") String userId) {
        logger.info("GET /api/v1/highlights/user/{}", userId);
        List<StoryHighlightDto> highlights = storyService.getUserHighlights(userId);
        return ResponseEntity.ok(highlights);
    }

    @GetMapping("/{highlightId}/stories")
    @Operation(summary = "Get highlight stories", description = "Get all stories in a highlight")
    public ResponseEntity<List<StoryDto>> getHighlightStories(
            @Parameter(description = "Highlight ID") @PathVariable("highlightId") UUID highlightId,
            @Parameter(description = "Viewer user ID (optional)") @RequestParam(value = "viewerUserId", required = false) String viewerUserId) {
        logger.info("GET /api/v1/highlights/{}/stories", highlightId);
        List<StoryDto> stories = storyService.getHighlightStories(highlightId, viewerUserId);
        return ResponseEntity.ok(stories);
    }

    @PostMapping("/{highlightId}/stories/{storyId}")
    @Operation(summary = "Add story to highlight", description = "Add a story to a highlight")
    public ResponseEntity<Void> addStoryToHighlight(
            @Parameter(description = "Highlight ID") @PathVariable("highlightId") UUID highlightId,
            @Parameter(description = "Story ID") @PathVariable("storyId") UUID storyId,
            @Parameter(description = "User ID", required = true) @RequestParam("userId") String userId) {
        logger.info("POST /api/v1/highlights/{}/stories/{} - userId={}", highlightId, storyId, userId);
        storyService.addStoryToHighlight(storyId, highlightId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{highlightId}/stories/{storyId}")
    @Operation(summary = "Remove story from highlight", description = "Remove a story from a highlight")
    public ResponseEntity<Void> removeStoryFromHighlight(
            @Parameter(description = "Highlight ID") @PathVariable("highlightId") UUID highlightId,
            @Parameter(description = "Story ID") @PathVariable("storyId") UUID storyId,
            @Parameter(description = "User ID", required = true) @RequestParam("userId") String userId) {
        logger.info("DELETE /api/v1/highlights/{}/stories/{} - userId={}", highlightId, storyId, userId);
        storyService.removeStoryFromHighlight(storyId, highlightId, userId);
        return ResponseEntity.noContent().build();
    }
}

