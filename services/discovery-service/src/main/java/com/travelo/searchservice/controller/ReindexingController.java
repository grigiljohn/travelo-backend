package com.travelo.searchservice.controller;

import com.travelo.searchservice.service.ReindexingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for re-indexing operations.
 * Allows manual triggering of re-indexing from source services to Elasticsearch.
 */
@RestController
@RequestMapping("/api/v1/admin/reindex")
@Tag(name = "Re-indexing", description = "Admin endpoints for re-indexing data to Elasticsearch")
public class ReindexingController {

    private static final Logger logger = LoggerFactory.getLogger(ReindexingController.class);

    private final ReindexingService reindexingService;

    public ReindexingController(ReindexingService reindexingService) {
        this.reindexingService = reindexingService;
    }

    @GetMapping
    @Operation(summary = "Re-indexing health check", description = "Verify re-indexing endpoints are available")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "Re-indexing endpoints are available");
        response.put("endpoints", List.of("/posts", "/reels", "/shops", "/products", "/users", "/all"));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/posts")
    @Operation(summary = "Re-index all posts", description = "Re-index all posts from post-service to Elasticsearch")
    public ResponseEntity<Map<String, Object>> reindexPosts() {
        logger.info("Re-indexing posts triggered via API");
        try {
            int count = reindexingService.reindexPosts();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Posts re-indexed successfully");
            response.put("postsIndexed", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error re-indexing posts: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error re-indexing posts: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/reels")
    @Operation(summary = "Re-index all reels", description = "Re-index all reels (posts with postType='reel') from post-service to Elasticsearch")
    public ResponseEntity<Map<String, Object>> reindexReels() {
        logger.info("Re-indexing reels triggered via API");
        try {
            int count = reindexingService.reindexReels();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reels re-indexed successfully");
            response.put("reelsIndexed", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error re-indexing reels: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error re-indexing reels: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/shops")
    @Operation(summary = "Re-index all shops", description = "Re-index all shops from shop-service to Elasticsearch")
    public ResponseEntity<Map<String, Object>> reindexShops() {
        logger.info("Re-indexing shops triggered via API");
        try {
            int count = reindexingService.reindexShops();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Shops re-indexed successfully");
            response.put("shopsIndexed", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error re-indexing shops: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error re-indexing shops: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/products")
    @Operation(summary = "Re-index all products", description = "Re-index all products from shop-service to Elasticsearch")
    public ResponseEntity<Map<String, Object>> reindexProducts() {
        logger.info("Re-indexing products triggered via API");
        try {
            int count = reindexingService.reindexProducts();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Products re-indexed successfully");
            response.put("productsIndexed", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error re-indexing products: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error re-indexing products: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/users")
    @Operation(summary = "Re-index all users", description = "Re-index all users from auth-service database to Elasticsearch")
    public ResponseEntity<Map<String, Object>> reindexUsers() {
        logger.info("Re-indexing users triggered via API");
        try {
            int count = reindexingService.reindexUsers();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Users re-indexed successfully");
            response.put("usersIndexed", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error re-indexing users: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error re-indexing users: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/all")
    @Operation(summary = "Re-index all data", description = "Re-index all data (posts, shops, products) from source services to Elasticsearch")
    public ResponseEntity<Map<String, Object>> reindexAll() {
        logger.info("Full re-indexing triggered via API");
        try {
            ReindexingService.ReindexingResult result = reindexingService.reindexAll();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", result.getMessage());
            response.put("postsIndexed", result.getPostsIndexed());
            response.put("shopsIndexed", result.getShopsIndexed());
            response.put("productsIndexed", result.getProductsIndexed());
            response.put("errors", result.getErrors());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error during full re-indexing: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error during full re-indexing: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

