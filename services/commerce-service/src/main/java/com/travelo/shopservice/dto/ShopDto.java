package com.travelo.shopservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelo.shopservice.entity.Shop;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for Shop entity.
 */
public class ShopDto {
    
    @JsonProperty("id")
    private UUID id;
    
    @JsonProperty("business_account_id")
    private UUID businessAccountId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("profile_image_url")
    private String profileImageUrl;
    
    @JsonProperty("cover_image_url")
    private String coverImageUrl;
    
    @JsonProperty("website_url")
    private String websiteUrl;
    
    @JsonProperty("contact_email")
    private String contactEmail;
    
    @JsonProperty("contact_phone")
    private String contactPhone;
    
    @JsonProperty("is_active")
    private Boolean isActive;
    
    @JsonProperty("is_verified")
    private Boolean isVerified;
    
    @JsonProperty("follower_count")
    private Long followerCount;
    
    @JsonProperty("product_count")
    private Long productCount;
    
    @JsonProperty("total_sales")
    private Long totalSales;
    
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
    
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    public static ShopDto fromEntity(Shop shop) {
        ShopDto dto = new ShopDto();
        dto.setId(shop.getId());
        dto.setBusinessAccountId(shop.getBusinessAccountId());
        dto.setName(shop.getName());
        dto.setDescription(shop.getDescription());
        dto.setCategory(shop.getCategory());
        dto.setProfileImageUrl(shop.getProfileImageUrl());
        dto.setCoverImageUrl(shop.getCoverImageUrl());
        dto.setWebsiteUrl(shop.getWebsiteUrl());
        dto.setContactEmail(shop.getContactEmail());
        dto.setContactPhone(shop.getContactPhone());
        dto.setIsActive(shop.getIsActive());
        dto.setIsVerified(shop.getIsVerified());
        dto.setFollowerCount(shop.getFollowerCount());
        dto.setProductCount(shop.getProductCount());
        dto.setTotalSales(shop.getTotalSales());
        dto.setCreatedAt(shop.getCreatedAt());
        dto.setUpdatedAt(shop.getUpdatedAt());
        return dto;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBusinessAccountId() { return businessAccountId; }
    public void setBusinessAccountId(UUID businessAccountId) { this.businessAccountId = businessAccountId; }
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
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
    public Long getFollowerCount() { return followerCount; }
    public void setFollowerCount(Long followerCount) { this.followerCount = followerCount; }
    public Long getProductCount() { return productCount; }
    public void setProductCount(Long productCount) { this.productCount = productCount; }
    public Long getTotalSales() { return totalSales; }
    public void setTotalSales(Long totalSales) { this.totalSales = totalSales; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}

