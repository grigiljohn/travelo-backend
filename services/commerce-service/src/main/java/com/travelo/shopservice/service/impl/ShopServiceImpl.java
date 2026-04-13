package com.travelo.shopservice.service.impl;

import com.travelo.shopservice.dto.*;
import com.travelo.shopservice.entity.Shop;
import com.travelo.shopservice.entity.Product;
import com.travelo.shopservice.repository.ShopRepository;
import com.travelo.shopservice.repository.ProductRepository;
import com.travelo.shopservice.service.ShopService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of ShopService.
 */
@Service
@Transactional(transactionManager = "shopTransactionManager")
public class ShopServiceImpl implements ShopService {

    private static final Logger logger = LoggerFactory.getLogger(ShopServiceImpl.class);

    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ShopServiceImpl(
            ShopRepository shopRepository,
            ProductRepository productRepository,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @Transactional(transactionManager = "shopTransactionManager")
    public ShopDto createShop(String userId, CreateShopRequest request) {
        logger.info("Creating shop for userId: {}, businessAccountId: {}", userId, request.getBusinessAccountId());

        // TODO: Validate that businessAccountId exists in ad-service
        // For now, we assume it exists

        // Check if shop already exists for this business account
        if (shopRepository.existsByBusinessAccountId(request.getBusinessAccountId())) {
            throw new IllegalArgumentException("Shop already exists for this business account");
        }

        Shop shop = new Shop();
        shop.setBusinessAccountId(request.getBusinessAccountId());
        shop.setName(request.getName());
        shop.setDescription(request.getDescription());
        shop.setCategory(request.getCategory());
        shop.setProfileImageUrl(request.getProfileImageUrl());
        shop.setCoverImageUrl(request.getCoverImageUrl());
        shop.setWebsiteUrl(request.getWebsiteUrl());
        shop.setContactEmail(request.getContactEmail());
        shop.setContactPhone(request.getContactPhone());
        shop.setIsActive(true);
        shop.setIsVerified(false);

        Shop savedShop = shopRepository.save(shop);
        logger.info("Shop created successfully: {}", savedShop.getId());

        // Publish Kafka event for search indexing
        publishShopCreatedEvent(savedShop);

        return ShopDto.fromEntity(savedShop);
    }

    @Override
    public ShopDto getShopById(UUID shopId, String viewerId) {
        logger.debug("Getting shop by ID: {}, viewerId: {}", shopId, viewerId);

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Shop not found: " + shopId));

        // TODO: Apply privacy rules if needed (e.g., hide inactive shops from non-owners)
        if (!shop.getIsActive() && (viewerId == null || !isShopOwner(shop, viewerId))) {
            throw new IllegalArgumentException("Shop not found or not accessible");
        }

        // Update product count
        long productCount = productRepository.countByShopIdAndIsAvailableTrue(shopId);
        shop.setProductCount(productCount);

        return ShopDto.fromEntity(shop);
    }

    @Override
    public ShopDto getShopByBusinessAccountId(UUID businessAccountId, String viewerId) {
        logger.debug("Getting shop by businessAccountId: {}, viewerId: {}", businessAccountId, viewerId);

        Shop shop = shopRepository.findByBusinessAccountId(businessAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Shop not found for business account: " + businessAccountId));

        // TODO: Apply privacy rules if needed
        if (!shop.getIsActive() && (viewerId == null || !isShopOwner(shop, viewerId))) {
            throw new IllegalArgumentException("Shop not found or not accessible");
        }

        // Update product count
        long productCount = productRepository.countByShopIdAndIsAvailableTrue(shop.getId());
        shop.setProductCount(productCount);

        return ShopDto.fromEntity(shop);
    }

    @Override
    @Transactional(transactionManager = "shopTransactionManager")
    public ShopDto updateShop(UUID shopId, String userId, UpdateShopRequest request) {
        logger.info("Updating shop: {}, userId: {}", shopId, userId);

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Shop not found: " + shopId));

        // TODO: Verify user owns this shop (check via businessAccountId -> userId mapping)
        // For now, we assume authorization is handled by the controller

        // Update fields if provided
        if (request.getName() != null) {
            shop.setName(request.getName());
        }
        if (request.getDescription() != null) {
            shop.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            shop.setCategory(request.getCategory());
        }
        if (request.getProfileImageUrl() != null) {
            shop.setProfileImageUrl(request.getProfileImageUrl());
        }
        if (request.getCoverImageUrl() != null) {
            shop.setCoverImageUrl(request.getCoverImageUrl());
        }
        if (request.getWebsiteUrl() != null) {
            shop.setWebsiteUrl(request.getWebsiteUrl());
        }
        if (request.getContactEmail() != null) {
            shop.setContactEmail(request.getContactEmail());
        }
        if (request.getContactPhone() != null) {
            shop.setContactPhone(request.getContactPhone());
        }
        if (request.getIsActive() != null) {
            shop.setIsActive(request.getIsActive());
        }

        Shop updatedShop = shopRepository.save(shop);
        logger.info("Shop updated successfully: {}", shopId);

        // Publish Kafka event for search indexing
        publishShopUpdatedEvent(updatedShop);

        return ShopDto.fromEntity(updatedShop);
    }

    @Override
    @Transactional(transactionManager = "shopTransactionManager")
    public void deleteShop(UUID shopId, String userId) {
        logger.info("Deleting shop: {}, userId: {}", shopId, userId);

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Shop not found: " + shopId));

        // TODO: Verify user owns this shop
        // Soft delete by setting isActive = false
        shop.setIsActive(false);
        shopRepository.save(shop);

        logger.info("Shop deleted (soft delete): {}", shopId);

        // Publish Kafka event for search indexing
        publishShopUpdatedEvent(shop);
    }

    @Override
    public PageResponse<ProductDto> getShopProducts(UUID shopId, int page, int limit, String category, Boolean featured) {
        logger.debug("Getting products for shop: {}, page: {}, limit: {}, category: {}, featured: {}", 
                shopId, page, limit, category, featured);

        // Verify shop exists and is active
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Shop not found: " + shopId));

        if (!shop.getIsActive()) {
            throw new IllegalArgumentException("Shop is not active");
        }

        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Product> productPage;

        if (featured != null && featured) {
            productPage = productRepository.findByShopIdAndIsFeaturedTrueAndIsAvailableTrue(shopId, pageable);
        } else if (category != null && !category.isEmpty()) {
            productPage = productRepository.findByShopIdAndCategoryAndIsAvailableTrue(shopId, category, pageable);
        } else {
            productPage = productRepository.findByShopIdAndIsAvailableTrue(shopId, pageable);
        }

        List<ProductDto> products = productPage.getContent().stream()
                .map(ProductDto::fromEntity)
                .collect(Collectors.toList());

        return new PageResponse<>(
                products,
                productPage.getTotalElements(),
                Integer.valueOf(page),
                Integer.valueOf(limit),
                Boolean.valueOf(productPage.hasNext())
        );
    }

    /**
     * Check if user owns the shop.
     * TODO: Implement by checking businessAccountId -> userId mapping via ad-service
     */
    private boolean isShopOwner(Shop shop, String userId) {
        // TODO: Call ad-service to verify userId owns businessAccountId
        return false; // Placeholder
    }

    /**
     * Publish shop.created Kafka event for search indexing.
     */
    private void publishShopCreatedEvent(Shop shop) {
        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("shopId", shop.getId().toString());
            event.put("businessAccountId", shop.getBusinessAccountId().toString());
            event.put("name", shop.getName());
            event.put("description", shop.getDescription());
            event.put("category", shop.getCategory());
            event.put("profileImageUrl", shop.getProfileImageUrl());
            event.put("coverImageUrl", shop.getCoverImageUrl());
            event.put("isActive", shop.getIsActive());
            event.put("isVerified", shop.getIsVerified());
            event.put("productCount", shop.getProductCount());
            event.put("followerCount", shop.getFollowerCount());

            kafkaTemplate.send("shop.created", shop.getId().toString(), event);
            logger.info("Published shop.created event for shop: {}", shop.getId());
        } catch (Exception e) {
            logger.error("Failed to publish shop.created event for shop {}: {}", shop.getId(), e.getMessage(), e);
            // Don't throw - event publishing failure shouldn't break shop creation
        }
    }

    /**
     * Publish shop.updated Kafka event for search indexing.
     */
    private void publishShopUpdatedEvent(Shop shop) {
        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("shopId", shop.getId().toString());
            event.put("businessAccountId", shop.getBusinessAccountId().toString());
            event.put("name", shop.getName());
            event.put("description", shop.getDescription());
            event.put("category", shop.getCategory());
            event.put("profileImageUrl", shop.getProfileImageUrl());
            event.put("coverImageUrl", shop.getCoverImageUrl());
            event.put("isActive", shop.getIsActive());
            event.put("isVerified", shop.getIsVerified());
            event.put("productCount", shop.getProductCount());
            event.put("followerCount", shop.getFollowerCount());

            kafkaTemplate.send("shop.updated", shop.getId().toString(), event);
            logger.info("Published shop.updated event for shop: {}", shop.getId());
        } catch (Exception e) {
            logger.error("Failed to publish shop.updated event for shop {}: {}", shop.getId(), e.getMessage(), e);
            // Don't throw - event publishing failure shouldn't break shop update
        }
    }
}

