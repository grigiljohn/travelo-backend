package com.travelo.shopservice.service;

import com.travelo.shopservice.dto.*;

import java.util.UUID;

/**
 * Service interface for shop management.
 */
public interface ShopService {
    
    /**
     * Create a new shop.
     */
    ShopDto createShop(String userId, CreateShopRequest request);
    
    /**
     * Get shop by ID.
     */
    ShopDto getShopById(UUID shopId, String viewerId);
    
    /**
     * Get shop by business account ID.
     */
    ShopDto getShopByBusinessAccountId(UUID businessAccountId, String viewerId);
    
    /**
     * Update shop.
     */
    ShopDto updateShop(UUID shopId, String userId, UpdateShopRequest request);
    
    /**
     * Delete shop (soft delete by setting isActive = false).
     */
    void deleteShop(UUID shopId, String userId);
    
    /**
     * Get shop products with pagination and filters.
     */
    PageResponse<ProductDto> getShopProducts(UUID shopId, int page, int limit, String category, Boolean featured);
}

