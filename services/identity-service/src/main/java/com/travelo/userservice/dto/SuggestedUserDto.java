package com.travelo.userservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class SuggestedUserDto {
    private UUID id;
    private String username;
    private String name;
    @JsonProperty("profile_picture_url")
    private String profilePictureUrl;
    private String role;
    @JsonProperty("is_following")
    private Boolean isFollowing;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Boolean getIsFollowing() { return isFollowing; }
    public void setIsFollowing(Boolean isFollowing) { this.isFollowing = isFollowing; }
}

