package com.travelo.shopservice.service;

import com.travelo.shopservice.dto.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service interface for product management.
 */
public interface ProductService {
    
    /**
     * Create a new product.
     */
    ProductDto createProduct(UUID shopId, String userId, CreateProductRequest request);
    
    /**
     * Get product by ID.
     */
    ProductDto getProductById(UUID productId);
    
    /**
     * Update product.
     */
    ProductDto updateProduct(UUID productId, String userId, UpdateProductRequest request);
    
    /**
     * Delete product.
     */
    void deleteProduct(UUID productId, String userId);
    
    /**
     * Increment product view count.
     */
    void incrementViewCount(UUID productId);
    
    /**
     * Like a product.
     */
    void likeProduct(UUID productId, String userId);
    
    /**
     * Unlike a product.
     */
    void unlikeProduct(UUID productId, String userId);
}

