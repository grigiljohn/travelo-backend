package com.travelo.postservice.controller;

import com.travelo.commons.security.SecurityUtils;
import com.travelo.postservice.dto.ApiResponse;
import com.travelo.postservice.dto.CreateDraftRequest;
import com.travelo.postservice.dto.PostDraftDto;
import com.travelo.postservice.service.PostDraftService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drafts")
public class PostDraftController {

    private static final Logger logger = LoggerFactory.getLogger(PostDraftController.class);
    private final PostDraftService draftService;

    public PostDraftController(PostDraftService draftService) {
        this.draftService = draftService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostDraftDto>> createDraft(
            @Valid @RequestBody CreateDraftRequest request) {
        // Extract user ID from JWT token (P0 security fix - use userId claim, not subject)
        String userId = SecurityUtils.getCurrentUserIdAsString();
        if (userId == null) {
            logger.warn("Unauthenticated attempt to create draft");
            throw new com.travelo.postservice.exception.UnauthorizedException("User not authenticated");
        }
        
        try {
            logger.info("Creating draft for user: {}", userId);

            PostDraftDto draft = draftService.createDraft(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Draft saved successfully", draft));
        } catch (Exception e) {
            logger.error("Error creating draft", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to save draft: " + e.getMessage(), "DRAFT_SAVE_ERROR"));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PostDraftDto>>> getUserDrafts() {
        // Extract user ID from JWT token (P0 security fix)
        String userId = SecurityUtils.getCurrentUserIdAsString();
        if (userId == null) {
            logger.warn("Unauthenticated attempt to fetch drafts");
            throw new com.travelo.postservice.exception.UnauthorizedException("User not authenticated");
        }
        
        try {
            logger.info("Fetching drafts for user: {}", userId);

            List<PostDraftDto> drafts = draftService.getUserDrafts(userId);
            return ResponseEntity.ok(ApiResponse.success("Drafts fetched successfully", drafts));
        } catch (Exception e) {
            logger.error("Error fetching drafts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch drafts: " + e.getMessage(), "DRAFT_FETCH_ERROR"));
        }
    }

    @GetMapping("/{draftId}")
    public ResponseEntity<ApiResponse<PostDraftDto>> getDraftById(
            @PathVariable UUID draftId) {
        // Extract user ID from JWT token (P0 security fix)
        String userId = SecurityUtils.getCurrentUserIdAsString();
        if (userId == null) {
            logger.warn("Unauthenticated attempt to fetch draft: {}", draftId);
            throw new com.travelo.postservice.exception.UnauthorizedException("User not authenticated");
        }
        
        try {
            logger.info("Fetching draft {} for user: {}", draftId, userId);

            PostDraftDto draft = draftService.getDraftById(draftId, userId);
            return ResponseEntity.ok(ApiResponse.success("Draft fetched successfully", draft));
        } catch (RuntimeException e) {
            logger.error("Error fetching draft", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), "DRAFT_NOT_FOUND"));
        } catch (Exception e) {
            logger.error("Error fetching draft", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch draft: " + e.getMessage(), "DRAFT_FETCH_ERROR"));
        }
    }

    @DeleteMapping("/{draftId}")
    public ResponseEntity<ApiResponse<Void>> deleteDraft(
            @PathVariable UUID draftId) {
        // Extract user ID from JWT token (P0 security fix)
        String userId = SecurityUtils.getCurrentUserIdAsString();
        if (userId == null) {
            logger.warn("Unauthenticated attempt to delete draft: {}", draftId);
            throw new com.travelo.postservice.exception.UnauthorizedException("User not authenticated");
        }
        
        try {
            logger.info("Deleting draft {} for user: {}", draftId, userId);

            draftService.deleteDraft(draftId, userId);
            return ResponseEntity.ok(ApiResponse.success("Draft deleted successfully", null));
        } catch (RuntimeException e) {
            logger.error("Error deleting draft", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), "DRAFT_NOT_FOUND"));
        } catch (Exception e) {
            logger.error("Error deleting draft", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete draft: " + e.getMessage(), "DRAFT_DELETE_ERROR"));
        }
    }

    @PostMapping("/{draftId}/schedule")
    public ResponseEntity<ApiResponse<PostDraftDto>> scheduleDraft(
            @PathVariable UUID draftId,
            @RequestBody Map<String, String> request) {
        // Extract user ID from JWT token (P0 security fix)
        String userId = SecurityUtils.getCurrentUserIdAsString();
        if (userId == null) {
            logger.warn("Unauthenticated attempt to schedule draft: {}", draftId);
            throw new com.travelo.postservice.exception.UnauthorizedException("User not authenticated");
        }
        
        try {
            String scheduledAtStr = request.get("scheduled_at");
            if (scheduledAtStr == null || scheduledAtStr.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("scheduled_at is required", "INVALID_REQUEST"));
            }

            OffsetDateTime scheduledAt = OffsetDateTime.parse(scheduledAtStr);
            logger.info("Scheduling draft {} for user: {} at {}", draftId, userId, scheduledAt);

            PostDraftDto draft = draftService.scheduleDraft(draftId, userId, scheduledAt);
            return ResponseEntity.ok(ApiResponse.success("Draft scheduled successfully", draft));
        } catch (RuntimeException e) {
            logger.error("Error scheduling draft", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), "DRAFT_NOT_FOUND"));
        } catch (Exception e) {
            logger.error("Error scheduling draft", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to schedule draft: " + e.getMessage(), "DRAFT_SCHEDULE_ERROR"));
        }
    }
}

