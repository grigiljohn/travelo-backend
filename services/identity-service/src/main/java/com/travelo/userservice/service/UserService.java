package com.travelo.userservice.service;

import com.travelo.userservice.dto.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface UserService {
    
    UserDto getUser(UUID userId, UUID viewerId);
    
    UserProfileDto getUserProfile(UUID userId, UUID viewerId);
    
    UserProfileDto updateUserProfile(UUID userId, UpdateUserProfileRequest request);

    UserProfileDto updateUserLocation(UUID userId, UpdateUserLocationRequest request);

    List<UserLocationEntryDto> getUserLocationHistory(UUID userId, int limit);
    
    FollowResponseDto followUser(UUID followerId, UUID followeeId);
    
    FollowResponseDto unfollowUser(UUID followerId, UUID followeeId);
    
    void blockUser(UUID blockerId, UUID blockedId);
    
    void unblockUser(UUID blockerId, UUID blockedId);
    
    List<UserDto> getBlockedUsers(UUID userId, int page, int limit);
    
    List<UserDto> searchUsers(String query, int page, int limit, UUID viewerId);
    
    List<SuggestedUserDto> getSuggestedUsers(UUID userId, int limit);
    
    Map<String, Object> getUserStats(UUID userId);

    /**
     * Users who follow {@code userId}. Each entry is a {@link UserDto} with follow state for {@code viewerId} when non-null.
     */
    List<UserDto> listFollowers(UUID userId, UUID viewerId, int page, int limit);

    /**
     * Users that {@code userId} follows.
     */
    List<UserDto> listFollowing(UUID userId, UUID viewerId, int page, int limit);

    long countUsers();
}

