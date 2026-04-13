package com.travelo.userservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public class UserDto {
    private UUID id;
    private String username;
    private String name;
    @JsonProperty("profile_picture_url")
    private String profilePictureUrl;
    @JsonProperty("is_verified")
    private Boolean isVerified;
    @JsonProperty("is_private")
    private Boolean isPrivate;
    @JsonProperty("is_following")
    private Boolean isFollowing;
    @JsonProperty("is_followed_by")
    private Boolean isFollowedBy;
    @JsonProperty("followers_count")
    private Long followersCount;
    @JsonProperty("following_count")
    private Long followingCount;
    @JsonProperty("posts_count")
    private Long postsCount;
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

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
    public Boolean getIsPrivate() { return isPrivate; }
    public void setIsPrivate(Boolean isPrivate) { this.isPrivate = isPrivate; }
    public Boolean getIsFollowing() { return isFollowing; }
    public void setIsFollowing(Boolean isFollowing) { this.isFollowing = isFollowing; }
    public Boolean getIsFollowedBy() { return isFollowedBy; }
    public void setIsFollowedBy(Boolean isFollowedBy) { this.isFollowedBy = isFollowedBy; }
    public Long getFollowersCount() { return followersCount; }
    public void setFollowersCount(Long followersCount) { this.followersCount = followersCount; }
    public Long getFollowingCount() { return followingCount; }
    public void setFollowingCount(Long followingCount) { this.followingCount = followingCount; }
    public Long getPostsCount() { return postsCount; }
    public void setPostsCount(Long postsCount) { this.postsCount = postsCount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}

