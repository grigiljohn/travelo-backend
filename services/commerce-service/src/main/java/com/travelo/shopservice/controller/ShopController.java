package com.travelo.shopservice.controller;

import com.travelo.shopservice.dto.*;
import com.travelo.shopservice.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for shop management endpoints.
 */
@RestController
@RequestMapping("/api/v1/shops")
@Tag(name = "Shops", description = "Shop management APIs")
public class ShopController {

    private static final Logger logger = LoggerFactory.getLogger(ShopController.class);

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @PostMapping
    @Operation(summary = "Create Shop", description = "Create a new shop for a business account")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<ShopDto>> createShop(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateShopRequest request) {
        
        logger.info("POST /api/v1/shops - userId: {}", userId);
        
        ShopDto shop = shopService.createShop(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shop created successfully", shop));
    }

    @GetMapping("/{shopId}")
    @Operation(summary = "Get Shop by ID", description = "Get shop details by shop ID")
    public ResponseEntity<ApiResponse<ShopDto>> getShop(
            @PathVariable UUID shopId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        
        logger.info("GET /api/v1/shops/{} - userId: {}", shopId, userId);
        
        ShopDto shop = shopService.getShopById(shopId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(shop));
    }

    @GetMapping("/business/{businessAccountId}")
    @Operation(summary = "Get Shop by Business Account", description = "Get shop details by business account ID")
    public ResponseEntity<ApiResponse<ShopDto>> getShopByBusinessAccount(
            @PathVariable UUID businessAccountId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        
        logger.info("GET /api/v1/shops/business/{} - userId: {}", businessAccountId, userId);
        
        ShopDto shop = shopService.getShopByBusinessAccountId(businessAccountId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(shop));
    }

    @PutMapping("/{shopId}")
    @Operation(summary = "Update Shop", description = "Update shop details")
    public ResponseEntity<ApiResponse<ShopDto>> updateShop(
            @PathVariable UUID shopId,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UpdateShopRequest request) {
        
        logger.info("PUT /api/v1/shops/{} - userId: {}", shopId, userId);
        
        ShopDto shop = shopService.updateShop(shopId, userId, request);
        
        return ResponseEntity.ok(ApiResponse.success("Shop updated successfully", shop));
    }

    @DeleteMapping("/{shopId}")
    @Operation(summary = "Delete Shop", description = "Delete (soft delete) a shop")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteShop(
            @PathVariable UUID shopId,
            @RequestHeader("X-User-Id") String userId) {
        
        logger.info("DELETE /api/v1/shops/{} - userId: {}", shopId, userId);
        
        shopService.deleteShop(shopId, userId);
        
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{shopId}/products")
    @Operation(summary = "Get Shop Products", description = "Get products for a shop with pagination and filters")
    public ResponseEntity<ApiResponse<PageResponse<ProductDto>>> getShopProducts(
            @PathVariable UUID shopId,
            @Parameter(description = "Page number", example = "1")
            @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(value = "limit", defaultValue = "20") @Max(100) int limit,
            @Parameter(description = "Filter by category")
            @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "Filter featured products only")
            @RequestParam(value = "featured", required = false) Boolean featured,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        
        logger.info("GET /api/v1/shops/{}/products - page: {}, limit: {}, category: {}, featured: {}", 
                shopId, page, limit, category, featured);
        
        PageResponse<ProductDto> products = shopService.getShopProducts(shopId, page, limit, category, featured);
        
        return ResponseEntity.ok(ApiResponse.success(products));
    }
}

