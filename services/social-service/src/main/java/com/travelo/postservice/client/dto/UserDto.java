package com.travelo.postservice.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/**
 * DTO for user information from user-service.
 * Used by UserServiceClient to fetch user details.
 */
public class UserDto {
    private UUID id;
    private String username;
    private String name;
    @JsonProperty("profile_picture_url")
    private String profilePictureUrl;
    @JsonProperty("is_verified")
    private Boolean isVerified;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }
    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
}

