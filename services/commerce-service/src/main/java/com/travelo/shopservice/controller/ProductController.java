package com.travelo.shopservice.controller;

import com.travelo.shopservice.dto.*;
import com.travelo.shopservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for product management endpoints.
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product management APIs")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/shops/{shopId}")
    @Operation(summary = "Create Product", description = "Create a new product in a shop")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(
            @PathVariable UUID shopId,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateProductRequest request) {
        
        logger.info("POST /api/v1/products/shops/{} - userId: {}", shopId, userId);
        
        ProductDto product = productService.createProduct(shopId, userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", product));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get Product by ID", description = "Get product details by product ID")
    public ResponseEntity<ApiResponse<ProductDto>> getProduct(@PathVariable UUID productId) {
        
        logger.info("GET /api/v1/products/{}", productId);
        
        ProductDto product = productService.getProductById(productId);
        
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update Product", description = "Update product details")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @PathVariable UUID productId,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UpdateProductRequest request) {
        
        logger.info("PUT /api/v1/products/{} - userId: {}", productId, userId);
        
        ProductDto product = productService.updateProduct(productId, userId, request);
        
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", product));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Delete Product", description = "Delete a product")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteProduct(
            @PathVariable UUID productId,
            @RequestHeader("X-User-Id") String userId) {
        
        logger.info("DELETE /api/v1/products/{} - userId: {}", productId, userId);
        
        productService.deleteProduct(productId, userId);
        
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/view")
    @Operation(summary = "Increment Product View Count", description = "Track product view")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> incrementViewCount(@PathVariable UUID productId) {
        
        logger.debug("POST /api/v1/products/{}/view", productId);
        
        productService.incrementViewCount(productId);
        
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/like")
    @Operation(summary = "Like Product", description = "Like a product")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> likeProduct(
            @PathVariable UUID productId,
            @RequestHeader("X-User-Id") String userId) {
        
        logger.info("POST /api/v1/products/{}/like - userId: {}", productId, userId);
        
        productService.likeProduct(productId, userId);
        
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{productId}/like")
    @Operation(summary = "Unlike Product", description = "Unlike a product")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> unlikeProduct(
            @PathVariable UUID productId,
            @RequestHeader("X-User-Id") String userId) {
        
        logger.info("DELETE /api/v1/products/{}/like - userId: {}", productId, userId);
        
        productService.unlikeProduct(productId, userId);
        
        return ResponseEntity.noContent().build();
    }
}

