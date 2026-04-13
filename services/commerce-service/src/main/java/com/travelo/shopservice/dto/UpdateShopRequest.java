package com.travelo.shopservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a shop.
 * All fields are optional - only provided fields will be updated.
 */
public class UpdateShopRequest {
    
    @Size(min = 1, max = 255, message = "Shop name must be between 1 and 255 characters")
    @JsonProperty("name")
    private String name;
    
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    @JsonProperty("description")
    private String description;
    
    @Size(max = 100, message = "Category must not exceed 100 characters")
    @JsonProperty("category")
    private String category;
    
    @Size(max = 500, message = "Profile image URL must not exceed 500 characters")
    @JsonProperty("profile_image_url")
    private String profileImageUrl;
    
    @Size(max = 500, message = "Cover image URL must not exceed 500 characters")
    @JsonProperty("cover_image_url")
    private String coverImageUrl;
    
    @Size(max = 500, message = "Website URL must not exceed 500 characters")
    @JsonProperty("website_url")
    private String websiteUrl;
    
    @Size(max = 255, message = "Contact email must not exceed 255 characters")
    @JsonProperty("contact_email")
    private String contactEmail;
    
    @Size(max = 20, message = "Contact phone must not exceed 20 characters")
    @JsonProperty("contact_phone")
    private String contactPhone;
    
    @JsonProperty("is_active")
    private Boolean isActive;

    // Getters and Setters
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
}

