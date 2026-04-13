package com.travelo.userservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FollowResponseDto {
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("is_following")
    private Boolean isFollowing;
    
    @JsonProperty("followers_count")
    private Long followersCount;

    public FollowResponseDto() {
    }

    public FollowResponseDto(String message, Boolean isFollowing, Long followersCount) {
        this.message = message;
        this.isFollowing = isFollowing;
        this.followersCount = followersCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getIsFollowing() {
        return isFollowing;
    }

    public void setIsFollowing(Boolean isFollowing) {
        this.isFollowing = isFollowing;
    }

    public Long getFollowersCount() {
        return followersCount;
    }

    public void setFollowersCount(Long followersCount) {
        this.followersCount = followersCount;
    }
}

