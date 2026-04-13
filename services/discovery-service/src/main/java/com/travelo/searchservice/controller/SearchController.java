package com.travelo.searchservice.controller;

import com.travelo.searchservice.dto.GroupedSearchResponse;
import com.travelo.searchservice.dto.SearchResponse;
import com.travelo.searchservice.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for search endpoints.
 */
@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Search", description = "Full-text search APIs")
public class SearchController {

    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @Operation(summary = "Search", 
               description = "Full-text search across users, hashtags, locations, and posts. Returns grouped results by default.")
    public ResponseEntity<GroupedSearchResponse> search(
            @Parameter(description = "Search query", required = true)
            @RequestParam("q") String query,
            @Parameter(description = "Comma-separated types: users,hashtags,places,posts (or 'all' for all types)")
            @RequestParam(value = "types", required = false, defaultValue = "all") String types,
            @Parameter(description = "Page number", example = "1")
            @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(value = "limit", defaultValue = "20") @Max(50) int limit,
            @Parameter(description = "User ID for privacy filtering (optional)")
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        
        logger.info("GET /api/v1/search - q='{}', types={}, page={}, limit={}, userId={}", 
                query, types, page, limit, userId);
        
        GroupedSearchResponse response = searchService.searchGrouped(query, types, page, limit, userId);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    @Operation(summary = "Search Users", description = "Search users only")
    public ResponseEntity<SearchResponse> searchUsers(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") @Max(50) int limit,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        
        logger.info("GET /api/v1/search/users - q='{}', page={}, limit={}, userId={}", 
                query, page, limit, userId);
        
        SearchResponse response = searchService.searchUsers(query, page, limit, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/hashtags")
    @Operation(summary = "Search Hashtags", description = "Search hashtags only")
    public ResponseEntity<SearchResponse> searchHashtags(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") @Max(50) int limit) {
        
        logger.info("GET /api/v1/search/hashtags - q='{}', page={}, limit={}", query, page, limit);
        
        SearchResponse response = searchService.searchHashtags(query, page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/places")
    @Operation(summary = "Search Places", description = "Search locations/places only")
    public ResponseEntity<SearchResponse> searchPlaces(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") @Max(50) int limit) {
        
        logger.info("GET /api/v1/search/places - q='{}', page={}, limit={}", query, page, limit);
        
        SearchResponse response = searchService.searchLocations(query, page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/posts")
    @Operation(summary = "Search Posts", description = "Search posts only")
    public ResponseEntity<SearchResponse> searchPosts(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") @Max(50) int limit,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        
        logger.info("GET /api/v1/search/posts - q='{}', page={}, limit={}, userId={}", 
                query, page, limit, userId);
        
        SearchResponse response = searchService.searchPosts(query, page, limit, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reels")
    @Operation(summary = "Get Reels", description = "Get reels for explore/discovery feed (no search query required)")
    public ResponseEntity<Map<String, Object>> getReels(
            @Parameter(description = "Page number", example = "1")
            @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Page size", example = "18")
            @RequestParam(value = "limit", defaultValue = "18") int limit,
            @Parameter(description = "User ID for privacy filtering (optional)")
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        
        // Manual validation for limit (max 50)
        if (limit > 50) {
            limit = 50;
        }
        if (limit < 1) {
            limit = 18;
        }
        
        logger.info("GET /api/v1/search/reels - page={}, limit={}, userId={}", page, limit, userId);
        
        SearchResponse response = searchService.searchReels(page, limit, userId);
        
        // Wrap in expected format
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("data", response.getResults());
        result.put("total", response.getTotalResults());
        result.put("page", response.getPage());
        result.put("limit", response.getLimit());
        result.put("hasMore", response.getHasMore());
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/categories")
    @Operation(summary = "Get Search Categories", description = "Get available search categories")
    public ResponseEntity<Map<String, Object>> getCategories() {
        logger.info("GET /api/v1/search/categories");
        
        List<Map<String, Object>> categories = List.of(
                Map.of("id", "users", "name", "Accounts", "icon", "person", "color", "#4A9EFF"),
                Map.of("id", "hashtags", "name", "Tags", "icon", "tag", "color", "#51CF66"),
                Map.of("id", "places", "name", "Places", "icon", "location_on", "color", "#4A9EFF"),
                Map.of("id", "posts", "name", "Posts", "icon", "image", "color", "#FF6B6B"),
                Map.of("id", "shops", "name", "Shops", "icon", "storefront", "color", "#FFD93D"),
                Map.of("id", "products", "name", "Products", "icon", "shopping_bag", "color", "#FF6B6B")
        );
        
        return ResponseEntity.ok(Map.of("success", true, "data", categories));
    }

    @GetMapping("/feed/following")
    @Operation(summary = "Get Following Feed", description = "Get reels from users that the current user follows")
    public ResponseEntity<Map<String, Object>> getFollowingFeed(
            @Parameter(description = "Page number", example = "1")
            @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @Parameter(description = "User ID (required for following feed)")
            @RequestHeader(value = "X-User-Id", required = true) String userId) {
        
        if (limit > 50) limit = 50;
        if (limit < 1) limit = 20;
        
        logger.info("GET /api/v1/search/feed/following - page={}, limit={}, userId={}", page, limit, userId);
        
        SearchResponse response = searchService.getFollowingFeed(page, limit, userId);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("data", response.getResults());
        result.put("total", response.getTotalResults());
        result.put("page", response.getPage());
        result.put("limit", response.getLimit());
        result.put("hasMore", response.getHasMore());
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/feed/explore")
    @Operation(summary = "Get Explore Feed", description = "Get generic explore feed based on user preferences")
    public ResponseEntity<Map<String, Object>> getExploreFeed(
            @Parameter(description = "Page number", example = "1")
            @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @Parameter(description = "User ID for personalization (optional)")
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        
        if (limit > 50) limit = 50;
        if (limit < 1) limit = 20;
        
        logger.info("GET /api/v1/search/feed/explore - page={}, limit={}, userId={}", page, limit, userId);
        
        SearchResponse response = searchService.getExploreFeed(page, limit, userId);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("data", response.getResults());
        result.put("total", response.getTotalResults());
        result.put("page", response.getPage());
        result.put("limit", response.getLimit());
        result.put("hasMore", response.getHasMore());
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/feed/nearby")
    @Operation(summary = "Get Nearby Feed", description = "Get feed based on user location")
    public ResponseEntity<Map<String, Object>> getNearbyFeed(
            @Parameter(description = "Latitude")
            @RequestParam(value = "lat", required = false) Double latitude,
            @Parameter(description = "Longitude")
            @RequestParam(value = "lng", required = false) Double longitude,
            @Parameter(description = "Page number", example = "1")
            @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @Parameter(description = "User ID (optional)")
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        
        if (limit > 50) limit = 50;
        if (limit < 1) limit = 20;
        
        logger.info("GET /api/v1/search/feed/nearby - lat={}, lng={}, page={}, limit={}, userId={}", 
                latitude, longitude, page, limit, userId);
        
        SearchResponse response = searchService.getNearbyFeed(latitude, longitude, page, limit, userId);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("data", response.getResults());
        result.put("total", response.getTotalResults());
        result.put("page", response.getPage());
        result.put("limit", response.getLimit());
        result.put("hasMore", response.getHasMore());
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/users/suggestions")
    @Operation(summary = "Get User Suggestions", description = "Get users that the current user can follow")
    public ResponseEntity<Map<String, Object>> getUserSuggestions(
            @Parameter(description = "Page number", example = "1")
            @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @Parameter(description = "User ID (optional)")
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        
        if (limit > 50) limit = 50;
        if (limit < 1) limit = 20;
        
        logger.info("GET /api/v1/search/users/suggestions - page={}, limit={}, userId={}", page, limit, userId);
        
        SearchResponse response = searchService.getUserSuggestions(page, limit, userId);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("data", response.getResults());
        result.put("total", response.getTotalResults());
        result.put("page", response.getPage());
        result.put("limit", response.getLimit());
        result.put("hasMore", response.getHasMore());
        
        return ResponseEntity.ok(result);
    }
}

