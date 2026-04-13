package com.travelo.shopservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating a product.
 */
public class CreateProductRequest {
    
    @NotBlank(message = "Product name is required")
    @Size(min = 1, max = 255, message = "Product name must be between 1 and 255 characters")
    @JsonProperty("name")
    private String name;
    
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    @JsonProperty("description")
    private String description;
    
    @Size(max = 100, message = "Category must not exceed 100 characters")
    @JsonProperty("category")
    private String category;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @JsonProperty("price")
    private BigDecimal price;
    
    @Size(min = 3, max = 3, message = "Currency must be 3 characters (ISO code)")
    @JsonProperty("currency")
    private String currency = "USD";
    
    @DecimalMin(value = "0.01", message = "Compare at price must be greater than 0")
    @JsonProperty("compare_at_price")
    private BigDecimal compareAtPrice;
    
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    @JsonProperty("sku")
    private String sku;
    
    @JsonProperty("inventory_count")
    private Integer inventoryCount = 0;
    
    @JsonProperty("is_available")
    private Boolean isAvailable = true;
    
    @JsonProperty("is_featured")
    private Boolean isFeatured = false;
    
    @JsonProperty("images")
    private List<String> images;
    
    @Size(max = 500, message = "Thumbnail URL must not exceed 500 characters")
    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getCompareAtPrice() { return compareAtPrice; }
    public void setCompareAtPrice(BigDecimal compareAtPrice) { this.compareAtPrice = compareAtPrice; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public Integer getInventoryCount() { return inventoryCount; }
    public void setInventoryCount(Integer inventoryCount) { this.inventoryCount = inventoryCount; }
    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }
    public Boolean getIsFeatured() { return isFeatured; }
    public void setIsFeatured(Boolean isFeatured) { this.isFeatured = isFeatured; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
}

