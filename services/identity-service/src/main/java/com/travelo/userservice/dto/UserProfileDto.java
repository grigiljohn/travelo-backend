package com.travelo.userservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

public class UserProfileDto {
    private UUID id;
    private String username;
    private String name;
    private String bio;
    @JsonProperty("profile_picture_url")
    private String profilePictureUrl;
    @JsonProperty("cover_photo_url")
    private String coverPhotoUrl;
    private String email;
    private String phone;
    @JsonProperty("is_private")
    private Boolean isPrivate;
    @JsonProperty("is_verified")
    private Boolean isVerified;
    @JsonProperty("followers_count")
    private Long followersCount;
    @JsonProperty("following_count")
    private Long followingCount;
    @JsonProperty("posts_count")
    private Long postsCount;
    @JsonProperty("likes_and_saves_count")
    private Long likesAndSavesCount;
    @JsonProperty("draft_count")
    private Long draftCount;
    @JsonProperty("ip_address")
    private String ipAddress;
    @JsonProperty("is_following")
    private Boolean isFollowing;
    @JsonProperty("is_followed_by")
    private Boolean isFollowedBy;
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
    @JsonProperty("location_required")
    private Boolean locationRequired;
    @JsonProperty("location_permission_granted")
    private Boolean locationPermissionGranted;
    @JsonProperty("current_location")
    private UserLocationEntryDto currentLocation;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }
    public String getCoverPhotoUrl() { return coverPhotoUrl; }
    public void setCoverPhotoUrl(String coverPhotoUrl) { this.coverPhotoUrl = coverPhotoUrl; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Boolean getIsPrivate() { return isPrivate; }
    public void setIsPrivate(Boolean isPrivate) { this.isPrivate = isPrivate; }
    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
    public Long getFollowersCount() { return followersCount; }
    public void setFollowersCount(Long followersCount) { this.followersCount = followersCount; }
    public Long getFollowingCount() { return followingCount; }
    public void setFollowingCount(Long followingCount) { this.followingCount = followingCount; }
    public Long getPostsCount() { return postsCount; }
    public void setPostsCount(Long postsCount) { this.postsCount = postsCount; }
    public Long getLikesAndSavesCount() { return likesAndSavesCount; }
    public void setLikesAndSavesCount(Long likesAndSavesCount) { this.likesAndSavesCount = likesAndSavesCount; }
    public Long getDraftCount() { return draftCount; }
    public void setDraftCount(Long draftCount) { this.draftCount = draftCount; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public Boolean getIsFollowing() { return isFollowing; }
    public void setIsFollowing(Boolean isFollowing) { this.isFollowing = isFollowing; }
    public Boolean getIsFollowedBy() { return isFollowedBy; }
    public void setIsFollowedBy(Boolean isFollowedBy) { this.isFollowedBy = isFollowedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Boolean getLocationRequired() { return locationRequired; }
    public void setLocationRequired(Boolean locationRequired) { this.locationRequired = locationRequired; }
    public Boolean getLocationPermissionGranted() { return locationPermissionGranted; }
    public void setLocationPermissionGranted(Boolean locationPermissionGranted) { this.locationPermissionGranted = locationPermissionGranted; }
    public UserLocationEntryDto getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(UserLocationEntryDto currentLocation) { this.currentLocation = currentLocation; }
}

