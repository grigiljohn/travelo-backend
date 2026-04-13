package com.travelo.searchservice.client;

import com.travelo.commons.config.ResilientWebClientConfig;
import com.travelo.searchservice.client.dto.ApiResponse;
import com.travelo.searchservice.client.dto.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

/**
 * Client to fetch shop and product data from shop-service for re-indexing.
 */
@Component
public class ShopServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ShopServiceClient.class);

    private final WebClient webClient;
    private final String shopServiceUrl;

    public ShopServiceClient(ResilientWebClientConfig resilientWebClientConfig,
                            @Value("${app.shop-service.url:http://localhost:8089}") String shopServiceUrl) {
        this.shopServiceUrl = shopServiceUrl;
        this.webClient = resilientWebClientConfig.createResilientWebClient("shop-service", shopServiceUrl);
    }

    /**
     * Fetch all shops with pagination.
     */
    public List<ShopDto> getAllShops(int page, int limit) {
        try {
            logger.debug("Fetching shops - page: {}, limit: {}", page, limit);

            ApiResponse<PageResponse<ShopDto>> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/shops")
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<PageResponse<ShopDto>>>() {})
                    .block();

            if (response != null && response.getData() != null && response.getData().getData() != null) {
                return response.getData().getData();
            }
            return List.of();
        } catch (WebClientResponseException e) {
            logger.error("Error fetching shops from shop-service: status={}", e.getStatusCode(), e);
            return List.of();
        } catch (Exception e) {
            logger.error("Error fetching shops: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Fetch all products for a shop with pagination.
     */
    public List<ProductDto> getShopProducts(String shopId, int page, int limit) {
        try {
            logger.debug("Fetching products for shop {} - page: {}, limit: {}", shopId, page, limit);

            ApiResponse<PageResponse<ProductDto>> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/shops/{shopId}/products")
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build(shopId))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<PageResponse<ProductDto>>>() {})
                    .block();

            if (response != null && response.getData() != null && response.getData().getData() != null) {
                return response.getData().getData();
            }
            return List.of();
        } catch (WebClientResponseException e) {
            logger.error("Error fetching products from shop-service: status={}", e.getStatusCode(), e);
            return List.of();
        } catch (Exception e) {
            logger.error("Error fetching products: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Shop DTO for re-indexing.
     */
    public static class ShopDto {
        private String id;
        private String businessAccountId;
        private String name;
        private String description;
        private String category;
        private String profileImageUrl;
        private String coverImageUrl;
        private Boolean isActive;
        private Boolean isVerified;
        private Long productCount;
        private Long followerCount;
        private String createdAt;
        private String updatedAt;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getBusinessAccountId() { return businessAccountId; }
        public void setBusinessAccountId(String businessAccountId) { this.businessAccountId = businessAccountId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getProfileImageUrl() { return profileImageUrl; }
        public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
        public String getCoverImageUrl() { return coverImageUrl; }
        public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        public Boolean getIsVerified() { return isVerified; }
        public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
        public Long getProductCount() { return productCount; }
        public void setProductCount(Long productCount) { this.productCount = productCount; }
        public Long getFollowerCount() { return followerCount; }
        public void setFollowerCount(Long followerCount) { this.followerCount = followerCount; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    /**
     * Product DTO for re-indexing.
     */
    public static class ProductDto {
        private String id;
        private String shopId;
        private String name;
        private String description;
        private String category;
        private Double price;
        private String currency;
        private String thumbnailUrl;
        private Boolean isAvailable;
        private Boolean isFeatured;
        private Long viewCount;
        private Long likeCount;
        private String createdAt;
        private String updatedAt;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getShopId() { return shopId; }
        public void setShopId(String shopId) { this.shopId = shopId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getThumbnailUrl() { return thumbnailUrl; }
        public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
        public Boolean getIsAvailable() { return isAvailable; }
        public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }
        public Boolean getIsFeatured() { return isFeatured; }
        public void setIsFeatured(Boolean isFeatured) { this.isFeatured = isFeatured; }
        public Long getViewCount() { return viewCount; }
        public void setViewCount(Long viewCount) { this.viewCount = viewCount; }
        public Long getLikeCount() { return likeCount; }
        public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }
}

