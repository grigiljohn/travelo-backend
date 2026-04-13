package com.travelo.messagingservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class ChatUserDto {
    private UUID id;
    private String username;
    private String name;
    @JsonProperty("profile_picture_url")
    private String profilePictureUrl;
    @JsonProperty("is_verified")
    private Boolean isVerified;
    @JsonProperty("is_online")
    private Boolean isOnline;
    @JsonProperty("is_following")
    private Boolean isFollowing;

    public ChatUserDto() {
    }

    public ChatUserDto(UUID id, String username, String name, String profilePictureUrl, Boolean isVerified, Boolean isOnline, Boolean isFollowing) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.profilePictureUrl = profilePictureUrl;
        this.isVerified = isVerified;
        this.isOnline = isOnline;
        this.isFollowing = isFollowing;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public Boolean getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(Boolean isOnline) {
        this.isOnline = isOnline;
    }

    public Boolean getIsFollowing() {
        return isFollowing;
    }

    public void setIsFollowing(Boolean isFollowing) {
        this.isFollowing = isFollowing;
    }
}

