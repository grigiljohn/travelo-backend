package com.travelo.userservice.controller;

import com.travelo.userservice.dto.*;
import com.travelo.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.ExceptionHandler;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User profile and management APIs")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}/profile")
    @Operation(summary = "Get user profile", description = "Get detailed user profile")
    public ResponseEntity<UserProfileDto> getUserProfile(
            @PathVariable("userId") UUID userId,
            @RequestParam(value = "viewer_id", required = false) UUID viewerId) {
        logger.info("GET /api/v1/users/{}/profile", userId);
        return ResponseEntity.ok(userService.getUserProfile(userId, viewerId));
    }

    @PutMapping("/{userId}/profile")
    @Operation(summary = "Update user profile", description = "Update user profile information")
    public ResponseEntity<UserProfileDto> updateUserProfile(
            @PathVariable("userId") UUID userId,
            @RequestHeader("X-User-Id") UUID authenticatedUserId,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        logger.info("PUT /api/v1/users/{}/profile", userId);
        
        // Verify user can only update their own profile
        if (!userId.equals(authenticatedUserId)) {
            throw new IllegalArgumentException("User can only update their own profile");
        }
        
        return ResponseEntity.ok(userService.updateUserProfile(userId, request));
    }

    @PutMapping("/{userId}/location")
    @Operation(summary = "Update user location", description = "Persist latest location on profile and append to location history.")
    public ResponseEntity<UserProfileDto> updateUserLocation(
            @PathVariable("userId") UUID userId,
            @RequestHeader("X-User-Id") UUID authenticatedUserId,
            @Valid @RequestBody UpdateUserLocationRequest request) {
        logger.info("PUT /api/v1/users/{}/location", userId);
        if (!userId.equals(authenticatedUserId)) {
            throw new IllegalArgumentException("User can only update their own location");
        }
        return ResponseEntity.ok(userService.updateUserLocation(userId, request));
    }

    @GetMapping("/{userId}/locations")
    @Operation(summary = "Get user location history", description = "Return latest recorded location entries for the user.")
    public ResponseEntity<List<UserLocationEntryDto>> getUserLocationHistory(
            @PathVariable("userId") UUID userId,
            @RequestHeader("X-User-Id") UUID authenticatedUserId,
            @RequestParam(value = "limit", defaultValue = "30") @Max(200) int limit) {
        logger.info("GET /api/v1/users/{}/locations", userId);
        if (!userId.equals(authenticatedUserId)) {
            throw new IllegalArgumentException("User can only view their own location history");
        }
        return ResponseEntity.ok(userService.getUserLocationHistory(userId, limit));
    }

    @PostMapping("/{userId}/follow")
    @Operation(summary = "Follow user", description = "Follow a user. Returns follow state and updated follower count.")
    public ResponseEntity<FollowResponseDto> followUser(
            @PathVariable("userId") UUID userId,
            @RequestHeader("X-User-Id") UUID followerId) {
        logger.info("POST /api/v1/users/{}/follow by user {}", userId, followerId);
        
        // Validate user ID format (handled by Spring)
        FollowResponseDto response = userService.followUser(followerId, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}/follow")
    @Operation(summary = "Unfollow user", description = "Unfollow a user. Returns follow state and updated follower count.")
    public ResponseEntity<FollowResponseDto> unfollowUser(
            @PathVariable("userId") UUID userId,
            @RequestHeader("X-User-Id") UUID followerId) {
        logger.info("DELETE /api/v1/users/{}/follow by user {}", userId, followerId);
        
        // Validate user ID format (handled by Spring)
        FollowResponseDto response = userService.unfollowUser(followerId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/block")
    @Operation(summary = "Block user", description = "Block a user")
    public ResponseEntity<Map<String, String>> blockUser(
            @PathVariable("userId") UUID userId,
            @RequestHeader("X-User-Id") UUID blockerId) {
        logger.info("POST /api/v1/users/{}/block", userId);
        userService.blockUser(blockerId, userId);
        return ResponseEntity.ok(Map.of("message", "User blocked successfully"));
    }

    @DeleteMapping("/{userId}/block")
    @Operation(summary = "Unblock user", description = "Unblock a user")
    public ResponseEntity<Map<String, String>> unblockUser(
            @PathVariable("userId") UUID userId,
            @RequestHeader("X-User-Id") UUID blockerId) {
        logger.info("DELETE /api/v1/users/{}/block", userId);
        userService.unblockUser(blockerId, userId);
        return ResponseEntity.ok(Map.of("message", "User unblocked successfully"));
    }

    @GetMapping("/{userId}/blocked")
    @Operation(summary = "Get blocked users", description = "Get list of users blocked by the authenticated user")
    public ResponseEntity<List<UserDto>> getBlockedUsers(
            @PathVariable("userId") UUID userId,
            @RequestHeader("X-User-Id") UUID authenticatedUserId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") @Max(100) int limit) {
        logger.info("GET /api/v1/users/{}/blocked", userId);
        
        // Verify user can only view their own blocked list
        if (!userId.equals(authenticatedUserId)) {
            throw new IllegalArgumentException("User can only view their own blocked users");
        }
        
        return ResponseEntity.ok(userService.getBlockedUsers(userId, page, limit));
    }

    @GetMapping("/discover/quick-tags")
    @Operation(summary = "Discover quick interest tags", description = "Short labels travelers can tap to seed user search (curated list).")
    public ResponseEntity<List<String>> discoverQuickTags() {
        return ResponseEntity.ok(List.of(
                "Weekend hikers",
                "Food tours",
                "Digital nomads",
                "Photography"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Search users by username or name. Includes follow state if viewer_id is provided.")
    public ResponseEntity<List<UserDto>> searchUsers(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") @Max(100) int limit,
            @RequestParam(value = "viewer_id", required = false) UUID viewerId) {
        logger.info("GET /api/v1/users/search - q={}, viewerId={}", query, viewerId);
        return ResponseEntity.ok(userService.searchUsers(query, page, limit, viewerId));
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get suggested users", description = "Get suggested users for the authenticated user")
    public ResponseEntity<List<SuggestedUserDto>> getSuggestedUsers(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(value = "limit", defaultValue = "10") @Max(50) int limit) {
        logger.info("GET /api/v1/users/suggestions for user {}", userId);
        return ResponseEntity.ok(userService.getSuggestedUsers(userId, limit));
    }

    @GetMapping("/{userId}/stats")
    @Operation(summary = "Get user stats", description = "Get additional user statistics (draft count, likes & saves count)")
    public ResponseEntity<Map<String, Object>> getUserStats(
            @PathVariable("userId") UUID userId) {
        logger.info("GET /api/v1/users/{}/stats", userId);
        return ResponseEntity.ok(userService.getUserStats(userId));
    }

    @GetMapping("/{userId}/followers")
    @Operation(summary = "List followers", description = "Paginated list of users who follow the given user.")
    public ResponseEntity<List<UserDto>> listFollowers(
            @PathVariable("userId") UUID userId,
            @RequestParam(value = "viewer_id", required = false) UUID viewerId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") @Max(100) int limit) {
        logger.info("GET /api/v1/users/{}/followers page={} limit={}", userId, page, limit);
        return ResponseEntity.ok(userService.listFollowers(userId, viewerId, page, limit));
    }

    @GetMapping("/{userId}/following")
    @Operation(summary = "List following", description = "Paginated list of users the given user follows.")
    public ResponseEntity<List<UserDto>> listFollowing(
            @PathVariable("userId") UUID userId,
            @RequestParam(value = "viewer_id", required = false) UUID viewerId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") @Max(100) int limit) {
        logger.info("GET /api/v1/users/{}/following page={} limit={}", userId, page, limit);
        return ResponseEntity.ok(userService.listFollowing(userId, viewerId, page, limit));
    }

    /** Declared after literal paths (`/search`, `/suggestions`, …) so they are not captured as UUIDs. */
    @GetMapping("/{userId}")
    @Operation(summary = "Get user", description = "Get user information by ID")
    public ResponseEntity<UserDto> getUser(
            @PathVariable("userId") UUID userId,
            @RequestParam(value = "viewer_id", required = false) UUID viewerId) {
        logger.info("GET /api/v1/users/{}", userId);
        return ResponseEntity.ok(userService.getUser(userId, viewerId));
    }

    @GetMapping("/metrics/counts")
    @Operation(summary = "User metrics counts", description = "Internal metrics endpoint for admin dashboards")
    public ResponseEntity<Map<String, Object>> counts() {
        return ResponseEntity.ok(Map.of("usersTotal", userService.countUsers()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.error("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}

