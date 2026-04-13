package com.travelo.shopservice.service.impl;

import com.travelo.shopservice.dto.*;
import com.travelo.shopservice.entity.Product;
import com.travelo.shopservice.entity.Shop;
import com.travelo.shopservice.repository.ProductRepository;
import com.travelo.shopservice.repository.ShopRepository;
import com.travelo.shopservice.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Implementation of ProductService.
 */
@Service
@Transactional(transactionManager = "shopTransactionManager")
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ProductServiceImpl(
            ProductRepository productRepository,
            ShopRepository shopRepository,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.productRepository = productRepository;
        this.shopRepository = shopRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @Transactional(transactionManager = "shopTransactionManager")
    public ProductDto createProduct(UUID shopId, String userId, CreateProductRequest request) {
        logger.info("Creating product for shop: {}, userId: {}", shopId, userId);

        // Verify shop exists and user owns it
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Shop not found: " + shopId));

        if (!shop.getIsActive()) {
            throw new IllegalArgumentException("Shop is not active");
        }

        // TODO: Verify user owns this shop

        Product product = new Product();
        product.setShopId(shopId);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        product.setCompareAtPrice(request.getCompareAtPrice());
        product.setSku(request.getSku());
        product.setInventoryCount(request.getInventoryCount() != null ? request.getInventoryCount() : 0);
        product.setIsAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true);
        product.setIsFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false);
        product.setImages(request.getImages() != null ? request.getImages() : new ArrayList<>());
        product.setThumbnailUrl(request.getThumbnailUrl());

        Product savedProduct = productRepository.save(product);
        logger.info("Product created successfully: {}", savedProduct.getId());

        // Update shop product count
        long productCount = productRepository.countByShopIdAndIsAvailableTrue(shopId);
        shop.setProductCount(productCount);
        shopRepository.save(shop);

        // Publish Kafka event for search indexing
        publishProductCreatedEvent(savedProduct);

        return ProductDto.fromEntity(savedProduct);
    }

    @Override
    public ProductDto getProductById(UUID productId) {
        logger.debug("Getting product by ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        // Increment view count
        incrementViewCount(productId);

        return ProductDto.fromEntity(product);
    }

    @Override
    @Transactional(transactionManager = "shopTransactionManager")
    public ProductDto updateProduct(UUID productId, String userId, UpdateProductRequest request) {
        logger.info("Updating product: {}, userId: {}", productId, userId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        // TODO: Verify user owns the shop that owns this product

        // Update fields if provided
        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            product.setCategory(request.getCategory());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getCurrency() != null) {
            product.setCurrency(request.getCurrency());
        }
        if (request.getCompareAtPrice() != null) {
            product.setCompareAtPrice(request.getCompareAtPrice());
        }
        if (request.getSku() != null) {
            product.setSku(request.getSku());
        }
        if (request.getInventoryCount() != null) {
            product.setInventoryCount(request.getInventoryCount());
        }
        if (request.getIsAvailable() != null) {
            product.setIsAvailable(request.getIsAvailable());
        }
        if (request.getIsFeatured() != null) {
            product.setIsFeatured(request.getIsFeatured());
        }
        if (request.getImages() != null) {
            product.setImages(request.getImages());
        }
        if (request.getThumbnailUrl() != null) {
            product.setThumbnailUrl(request.getThumbnailUrl());
        }

        Product updatedProduct = productRepository.save(product);
        logger.info("Product updated successfully: {}", productId);

        // Publish Kafka event for search indexing
        publishProductUpdatedEvent(updatedProduct);

        return ProductDto.fromEntity(updatedProduct);
    }

    @Override
    @Transactional(transactionManager = "shopTransactionManager")
    public void deleteProduct(UUID productId, String userId) {
        logger.info("Deleting product: {}, userId: {}", productId, userId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        // TODO: Verify user owns the shop that owns this product

        UUID shopId = product.getShopId();
        productRepository.delete(product);

        // Update shop product count
        Shop shop = shopRepository.findById(shopId).orElse(null);
        if (shop != null) {
            long productCount = productRepository.countByShopIdAndIsAvailableTrue(shopId);
            shop.setProductCount(productCount);
            shopRepository.save(shop);
        }

        logger.info("Product deleted: {}", productId);

        // Publish Kafka event for search indexing
        publishProductDeletedEvent(productId);
    }

    @Override
    @Transactional(transactionManager = "shopTransactionManager")
    public void incrementViewCount(UUID productId) {
        productRepository.findById(productId).ifPresent(product -> {
            product.setViewCount(product.getViewCount() + 1);
            productRepository.save(product);
        });
    }

    @Override
    @Transactional(transactionManager = "shopTransactionManager")
    public void likeProduct(UUID productId, String userId) {
        logger.debug("Liking product: {}, userId: {}", productId, userId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        // TODO: Track which users liked which products (separate table for many-to-many)
        // For now, just increment like count
        product.setLikeCount(product.getLikeCount() + 1);
        productRepository.save(product);
    }

    @Override
    @Transactional(transactionManager = "shopTransactionManager")
    public void unlikeProduct(UUID productId, String userId) {
        logger.debug("Unliking product: {}, userId: {}", productId, userId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        // TODO: Track which users liked which products
        // For now, just decrement like count (with floor at 0)
        if (product.getLikeCount() > 0) {
            product.setLikeCount(product.getLikeCount() - 1);
            productRepository.save(product);
        }
    }

    /**
     * Publish product.created Kafka event for search indexing.
     */
    private void publishProductCreatedEvent(Product product) {
        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("productId", product.getId().toString());
            event.put("shopId", product.getShopId().toString());
            event.put("name", product.getName());
            event.put("description", product.getDescription());
            event.put("category", product.getCategory());
            event.put("price", product.getPrice());
            event.put("currency", product.getCurrency());
            event.put("thumbnailUrl", product.getThumbnailUrl());
            event.put("isAvailable", product.getIsAvailable());
            event.put("isFeatured", product.getIsFeatured());

            kafkaTemplate.send("product.created", product.getId().toString(), event);
            logger.info("Published product.created event for product: {}", product.getId());
        } catch (Exception e) {
            logger.error("Failed to publish product.created event for product {}: {}", product.getId(), e.getMessage(), e);
        }
    }

    /**
     * Publish product.updated Kafka event for search indexing.
     */
    private void publishProductUpdatedEvent(Product product) {
        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("productId", product.getId().toString());
            event.put("shopId", product.getShopId().toString());
            event.put("name", product.getName());
            event.put("description", product.getDescription());
            event.put("category", product.getCategory());
            event.put("price", product.getPrice());
            event.put("currency", product.getCurrency());
            event.put("thumbnailUrl", product.getThumbnailUrl());
            event.put("isAvailable", product.getIsAvailable());
            event.put("isFeatured", product.getIsFeatured());

            kafkaTemplate.send("product.updated", product.getId().toString(), event);
            logger.info("Published product.updated event for product: {}", product.getId());
        } catch (Exception e) {
            logger.error("Failed to publish product.updated event for product {}: {}", product.getId(), e.getMessage(), e);
        }
    }

    /**
     * Publish product.deleted Kafka event for search indexing.
     */
    private void publishProductDeletedEvent(UUID productId) {
        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("productId", productId.toString());

            kafkaTemplate.send("product.deleted", productId.toString(), event);
            logger.info("Published product.deleted event for product: {}", productId);
        } catch (Exception e) {
            logger.error("Failed to publish product.deleted event for product {}: {}", productId, e.getMessage(), e);
        }
    }
}

