package com.travelo.userservice.service.impl;

import com.travelo.userservice.dto.*;
import com.travelo.authservice.entity.User;
import com.travelo.userservice.entity.Follow;
import com.travelo.userservice.dto.SuggestedUserDto;
import com.travelo.userservice.dto.events.UserFollowedEvent;
import com.travelo.userservice.event.UserEventPublisher;
import com.travelo.authservice.repository.UserRepository;
import com.travelo.userservice.repository.FollowRepository;
import com.travelo.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final UserEventPublisher userEventPublisher;

    public UserServiceImpl(UserRepository userRepository,
                           FollowRepository followRepository,
                           UserEventPublisher userEventPublisher) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.userEventPublisher = userEventPublisher;
        logger.info("UserServiceImpl initialized with UserRepository, FollowRepository, UserEventPublisher");
    }

    @Override
    public UserDto getUser(UUID userId, UUID viewerId) {
        logger.info("Getting user: {} for viewer: {}", userId, viewerId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.warn("User not found in database: userId={}", userId);
            // Return a default user DTO with the userId
            UserDto user = new UserDto();
            user.setId(userId);
            user.setUsername("user_" + userId.toString().substring(0, 8));
            user.setName("Unknown User");
            user.setProfilePictureUrl(null);
            user.setIsVerified(false);
            user.setIsPrivate(false);
            user.setFollowersCount(0L);
            user.setFollowingCount(0L);
            user.setPostsCount(0L);
            user.setCreatedAt(OffsetDateTime.now());
            return user;
        }
        
        User userEntity = userOpt.get();
        UserDto user = new UserDto();
        user.setId(userEntity.getId());
        user.setUsername(userEntity.getUsername() != null && !userEntity.getUsername().isEmpty() 
                ? userEntity.getUsername() 
                : "user_" + userId.toString().substring(0, 8));
        user.setName(userEntity.getName() != null && !userEntity.getName().isEmpty() 
                ? userEntity.getName() 
                : "Unknown User");
        user.setProfilePictureUrl(userEntity.getProfilePictureUrl());
        user.setIsVerified(Boolean.TRUE.equals(userEntity.getIsEmailVerified()));
        user.setIsPrivate(Boolean.TRUE.equals(userEntity.getIsPrivate()));
        
        // Get follower/following counts with error handling (table might not exist)
        try {
            user.setFollowersCount(followRepository.countByFolloweeId(userId));
            user.setFollowingCount(followRepository.countByFollowerId(userId));
        } catch (Exception e) {
            logger.warn("Error getting follow counts for userId={}: {}. Follows table may not exist. Using default values.", userId, e.getMessage());
            user.setFollowersCount(0L);
            user.setFollowingCount(0L);
        }
        
        user.setPostsCount(0L); // TODO: Calculate from PostService
        user.setCreatedAt(userEntity.getCreatedAt() != null ? userEntity.getCreatedAt() : OffsetDateTime.now());
        
        // Check follow status if viewerId is provided
        if (viewerId != null) {
            try {
                user.setIsFollowing(followRepository.existsByFollowerIdAndFolloweeId(viewerId, userId));
                user.setIsFollowedBy(followRepository.existsByFollowerIdAndFolloweeId(userId, viewerId));
            } catch (Exception e) {
                logger.warn("Error checking follow status for userId={}, viewerId={}: {}. Using default values.", userId, viewerId, e.getMessage());
                user.setIsFollowing(false);
                user.setIsFollowedBy(false);
            }
        }
        
        logger.info("Retrieved user from database: userId={}, username={}, name={}", 
                userId, user.getUsername(), user.getName());
        return user;
    }

    @Override
    public UserProfileDto getUserProfile(UUID userId, UUID viewerId) {
        logger.info("Getting user profile: {} for viewer: {}", userId, viewerId);
        
        try {
            Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            logger.warn("User not found in database: userId={}", userId);
            // Return a default profile DTO with the userId
            UserProfileDto profile = new UserProfileDto();
            profile.setId(userId);
            profile.setUsername("user_" + userId.toString().substring(0, 8));
            profile.setName("Unknown User");
            profile.setBio(null);
            profile.setProfilePictureUrl(null);
            profile.setEmail(null);
            profile.setPhone(null);
            profile.setIsPrivate(false);
            profile.setIsVerified(false);
            profile.setFollowersCount(0L);
            profile.setFollowingCount(0L);
            profile.setPostsCount(0L);
            profile.setDraftCount(0L);
            profile.setLikesAndSavesCount(0L);
            profile.setIpAddress("Unknown");
            profile.setCoverPhotoUrl(null);
            profile.setCreatedAt(OffsetDateTime.now());
            profile.setUpdatedAt(OffsetDateTime.now());
            return profile;
        }
        
        User userEntity = userOpt.get();
        UserProfileDto profile = new UserProfileDto();
        profile.setId(userEntity.getId());
        profile.setUsername(userEntity.getUsername() != null ? userEntity.getUsername() : "user_" + userId.toString().substring(0, 8));
        profile.setName(userEntity.getName() != null ? userEntity.getName() : "Unknown User");
        profile.setBio(userEntity.getBio());
        profile.setProfilePictureUrl(userEntity.getProfilePictureUrl());
        profile.setCoverPhotoUrl(userEntity.getCoverPhotoUrl());
        profile.setEmail(userEntity.getEmail());
        profile.setPhone(userEntity.getMobile());
        profile.setIsPrivate(Boolean.TRUE.equals(userEntity.getIsPrivate()));
        profile.setIsVerified(Boolean.TRUE.equals(userEntity.getIsEmailVerified()));
        
        // Get follower/following counts with error handling (table might not exist)
        try {
            profile.setFollowersCount(followRepository.countByFolloweeId(userId));
            profile.setFollowingCount(followRepository.countByFollowerId(userId));
        } catch (Exception e) {
            logger.warn("Error getting follow counts for userId={}: {}. Follows table may not exist. Using default values.", userId, e.getMessage());
            profile.setFollowersCount(0L);
            profile.setFollowingCount(0L);
        }
        
        profile.setPostsCount(0L); // TODO: Calculate from PostService
        
        // Get additional stats with error handling
        try {
            Map<String, Object> stats = getUserStats(userId);
            Object draftCountObj = stats.getOrDefault("draft_count", 0L);
            Object likesAndSavesObj = stats.getOrDefault("likes_and_saves_count", 0L);
            Object ipAddressObj = stats.getOrDefault("ip_address", "Unknown");
            
            profile.setDraftCount(draftCountObj instanceof Long ? (Long) draftCountObj : 
                draftCountObj instanceof Number ? ((Number) draftCountObj).longValue() : 0L);
            profile.setLikesAndSavesCount(likesAndSavesObj instanceof Long ? (Long) likesAndSavesObj : 
                likesAndSavesObj instanceof Number ? ((Number) likesAndSavesObj).longValue() : 0L);
            profile.setIpAddress(ipAddressObj != null ? ipAddressObj.toString() : "Unknown");
        } catch (Exception e) {
            logger.error("Error getting user stats for userId={}: {}", userId, e.getMessage(), e);
            // Set default values if stats retrieval fails
            profile.setDraftCount(0L);
            profile.setLikesAndSavesCount(0L);
            profile.setIpAddress("Unknown");
        }
        
        profile.setCreatedAt(userEntity.getCreatedAt() != null ? userEntity.getCreatedAt() : OffsetDateTime.now());
        profile.setUpdatedAt(userEntity.getUpdatedAt() != null ? userEntity.getUpdatedAt() : OffsetDateTime.now());
        
        // Check follow status if viewerId is provided
        if (viewerId != null) {
            try {
                profile.setIsFollowing(followRepository.existsByFollowerIdAndFolloweeId(viewerId, userId));
                profile.setIsFollowedBy(followRepository.existsByFollowerIdAndFolloweeId(userId, viewerId));
            } catch (Exception e) {
                logger.warn("Error checking follow status for userId={}, viewerId={}: {}. Using default values.", userId, viewerId, e.getMessage());
                profile.setIsFollowing(false);
                profile.setIsFollowedBy(false);
            }
        }
        
        logger.info("Retrieved user profile from database: userId={}, username={}, name={}", 
                userId, profile.getUsername(), profile.getName());
        return profile;
        } catch (Exception e) {
            logger.error("Error getting user profile for userId={}, viewerId={}: {}", 
                    userId, viewerId, e.getMessage(), e);
            // Return a minimal profile DTO to prevent 500 error
            UserProfileDto profile = new UserProfileDto();
            profile.setId(userId);
            profile.setUsername("user_" + userId.toString().substring(0, 8));
            profile.setName("Unknown User");
            profile.setBio(null);
            profile.setProfilePictureUrl(null);
            profile.setCoverPhotoUrl(null);
            profile.setEmail(null);
            profile.setPhone(null);
            profile.setIsPrivate(false);
            profile.setIsVerified(false);
            profile.setFollowersCount(0L);
            profile.setFollowingCount(0L);
            profile.setPostsCount(0L);
            profile.setDraftCount(0L);
            profile.setLikesAndSavesCount(0L);
            profile.setIpAddress("Unknown");
            profile.setCreatedAt(OffsetDateTime.now());
            profile.setUpdatedAt(OffsetDateTime.now());
            return profile;
        }
    }

    @Override
    @Transactional
    public UserProfileDto updateUserProfile(UUID userId, UpdateUserProfileRequest request) {
        logger.info("Updating user profile: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio().isBlank() ? null : request.getBio().trim());
        }
        if (request.getProfilePictureUrl() != null) {
            String u = request.getProfilePictureUrl().trim();
            user.setProfilePictureUrl(u.isEmpty() ? null : u);
        }
        if (request.getCoverPhotoUrl() != null) {
            String u = request.getCoverPhotoUrl().trim();
            user.setCoverPhotoUrl(u.isEmpty() ? null : u);
        }
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String nu = request.getUsername().trim();
            if (!nu.equals(user.getUsername()) && userRepository.existsByUsername(nu)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already taken");
            }
            user.setUsername(nu);
        }
        if (request.getPhone() != null) {
            String p = request.getPhone().trim();
            user.setMobile(p.isEmpty() ? null : p);
        }
        if (request.getIsPrivate() != null) {
            user.setIsPrivate(request.getIsPrivate());
        }

        userRepository.save(user);
        return getUserProfile(userId, userId);
    }

    @Override
    @Transactional
    public FollowResponseDto followUser(UUID followerId, UUID followeeId) {
        logger.info("User {} following user {}", followerId, followeeId);
        
        // Prevent self-follow
        if (followerId.equals(followeeId)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Cannot follow yourself"
            );
        }
        
        // Check if follower exists
        Optional<User> followerOpt = userRepository.findById(followerId);
        if (followerOpt.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Follower user not found: " + followerId
            );
        }
        
        // Check if followee exists
        Optional<User> followeeOpt = userRepository.findById(followeeId);
        if (followeeOpt.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User to follow not found: " + followeeId
            );
        }
        
        // Check if already following
        boolean alreadyFollowing = followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
        if (alreadyFollowing) {
            logger.info("User {} already follows user {}", followerId, followeeId);
            // Return current state
            long followersCount = followRepository.countByFolloweeId(followeeId);
            return new FollowResponseDto("Already following this user", true, followersCount);
        }
        
        // Create follow relationship
        Follow follow = new Follow(followerId, followeeId);
        followRepository.save(follow);
        logger.info("Successfully created follow relationship: {} -> {}", followerId, followeeId);

        publishFollowEvent(followerId, followeeId, true);

        // Return updated state
        long followersCount = followRepository.countByFolloweeId(followeeId);
        return new FollowResponseDto("User followed successfully", true, followersCount);
    }

    @Override
    @Transactional
    public FollowResponseDto unfollowUser(UUID followerId, UUID followeeId) {
        logger.info("User {} unfollowing user {}", followerId, followeeId);
        
        // Prevent self-unfollow (though it's not an error, just return current state)
        if (followerId.equals(followeeId)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Cannot unfollow yourself"
            );
        }
        
        // Check if follow relationship exists
        boolean wasFollowing = followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
        if (!wasFollowing) {
            logger.info("User {} is not following user {}", followerId, followeeId);
            // Return current state (not an error)
            long followersCount = followRepository.countByFolloweeId(followeeId);
            return new FollowResponseDto("Not following this user", false, followersCount);
        }
        
        // Delete follow relationship
        followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
        logger.info("Successfully removed follow relationship: {} -> {}", followerId, followeeId);

        publishFollowEvent(followerId, followeeId, false);

        // Return updated state
        long followersCount = followRepository.countByFolloweeId(followeeId);
        return new FollowResponseDto("User unfollowed successfully", false, followersCount);
    }

    /**
     * Publish follow/unfollow event. Wrapped in try/catch so downstream failures
     * (e.g. broker unavailable) never roll back the user-visible follow action.
     */
    private void publishFollowEvent(UUID followerId, UUID followeeId, boolean followed) {
        try {
            UserFollowedEvent event = followed
                    ? UserFollowedEvent.followed(followerId.toString(), followeeId.toString())
                    : UserFollowedEvent.unfollowed(followerId.toString(), followeeId.toString());
            userEventPublisher.publishUserFollowed(event);
        } catch (Exception ex) {
            logger.warn("flow=user_follow_event_publish_failed followerId={} followeeId={} followed={} err={}",
                    followerId, followeeId, followed, ex.toString());
        }
    }

    @Override
    @Transactional
    public void blockUser(UUID blockerId, UUID blockedId) {
        logger.info("User {} blocking user {}", blockerId, blockedId);
        // TODO: Implement with BlockRepository
        // - Create block relationship
        // - Unfollow if following
        // - Remove from followers if blocked user is following
    }

    @Override
    @Transactional
    public void unblockUser(UUID blockerId, UUID blockedId) {
        logger.info("User {} unblocking user {}", blockerId, blockedId);
        // TODO: Implement with BlockRepository
        // - Remove block relationship
    }

    @Override
    public List<UserDto> getBlockedUsers(UUID userId, int page, int limit) {
        logger.info("Getting blocked users for user: {}, page: {}, limit: {}", userId, page, limit);
        // TODO: Implement with BlockRepository
        return new ArrayList<>();
    }

    @Override
    public List<UserDto> searchUsers(String query, int page, int limit, UUID viewerId) {
        logger.info("Searching users with query: {}, page: {}, limit: {}, viewerId: {}", query, page, limit, viewerId);
        
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // Search users by username or name
        List<User> users = userRepository.searchByUsernameOrName(query.trim());
        
        // Apply pagination
        int offset = (page - 1) * limit;
        int endIndex = Math.min(offset + limit, users.size());
        List<User> paginatedUsers = users.subList(Math.min(offset, users.size()), endIndex);
        
        // Convert to DTOs with follow state
        List<UserDto> userDtos = new ArrayList<>();
        for (User user : paginatedUsers) {
            UserDto dto = new UserDto();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername() != null ? user.getUsername() : "user_" + user.getId().toString().substring(0, 8));
            dto.setName(user.getName() != null ? user.getName() : "Unknown User");
            dto.setProfilePictureUrl(user.getProfilePictureUrl());
            dto.setIsVerified(Boolean.TRUE.equals(user.getIsEmailVerified()));
            dto.setIsPrivate(Boolean.TRUE.equals(user.getIsPrivate()));
            dto.setFollowersCount(followRepository.countByFolloweeId(user.getId()));
            dto.setFollowingCount(followRepository.countByFollowerId(user.getId()));
            dto.setPostsCount(0L); // TODO: Calculate from PostService
            dto.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt() : OffsetDateTime.now());
            
            // Set follow state if viewerId is provided
            if (viewerId != null) {
                dto.setIsFollowing(followRepository.existsByFollowerIdAndFolloweeId(viewerId, user.getId()));
                dto.setIsFollowedBy(followRepository.existsByFollowerIdAndFolloweeId(user.getId(), viewerId));
            }
            
            userDtos.add(dto);
        }
        
        logger.info("Found {} users matching query '{}' (page {}, limit {})", userDtos.size(), query, page, limit);
        return userDtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuggestedUserDto> getSuggestedUsers(UUID userId, int limit) {
        int capped = Math.max(1, Math.min(limit, 50));
        logger.info("Getting suggested users for user: {}, limit: {}", userId, capped);

        // 1. Direct neighborhood: who the viewer already follows.
        Set<UUID> directlyFollowed = new HashSet<>();
        for (Follow f : followRepository.findByFollowerId(userId)) {
            directlyFollowed.add(f.getFolloweeId());
        }

        // 2. Friends-of-friends: for each user the viewer follows, look at the
        //    people *they* follow. Count how many mutual paths each candidate
        //    has — that's the primary ranking signal (classic follow-graph
        //    recommendation: "N people you follow also follow X").
        Map<UUID, Integer> mutualFollowScore = new HashMap<>();
        for (UUID friendId : directlyFollowed) {
            List<Follow> friendsFollowees = followRepository.findByFollowerId(friendId);
            for (Follow f : friendsFollowees) {
                UUID candidate = f.getFolloweeId();
                if (candidate.equals(userId)) continue;
                if (directlyFollowed.contains(candidate)) continue;
                mutualFollowScore.merge(candidate, 1, Integer::sum);
            }
        }

        // 3. Rank by score desc, then by follower count desc (popularity tie
        //    breaker). We resolve user rows lazily to avoid pulling the whole
        //    table into memory.
        List<Map.Entry<UUID, Integer>> ranked = new ArrayList<>(mutualFollowScore.entrySet());
        ranked.sort(Comparator
                .comparingInt((Map.Entry<UUID, Integer> e) -> e.getValue()).reversed()
                .thenComparing(e -> e.getKey().toString()));

        LinkedHashMap<UUID, SuggestedUserDto> out = new LinkedHashMap<>();
        for (Map.Entry<UUID, Integer> entry : ranked) {
            if (out.size() >= capped) break;
            Optional<User> candidate = userRepository.findById(entry.getKey());
            candidate.ifPresent(u -> {
                SuggestedUserDto dto = buildSuggestionDto(u, entry.getValue(), followRepository.countByFolloweeId(u.getId()));
                out.put(u.getId(), dto);
            });
        }

        // 4. Fallback: if the friend-graph is too sparse to fill the slate
        //    (e.g. the viewer follows nobody yet), pad with the most popular
        //    users they don't already follow. This keeps the "You may be
        //    interested in" strip meaningful for brand-new accounts.
        if (out.size() < capped) {
            // Copy into a mutable list so we can sort without mutating any
            // caller-visible collection (Spring Data returns a mutable list
            // today, but unit tests and future repo implementations may pass
            // immutable ones).
            List<User> popular = new ArrayList<>(userRepository.findAll());
            popular.sort(Comparator
                    .comparingLong((User u) -> followRepository.countByFolloweeId(u.getId()))
                    .reversed());
            for (User u : popular) {
                if (out.size() >= capped) break;
                if (u.getId().equals(userId)) continue;
                if (directlyFollowed.contains(u.getId())) continue;
                if (out.containsKey(u.getId())) continue;
                out.put(u.getId(), buildSuggestionDto(u, 0, followRepository.countByFolloweeId(u.getId())));
            }
        }

        List<SuggestedUserDto> suggestions = new ArrayList<>(out.values());
        logger.info("Suggested users for {} -> returned={} (fromGraph={}, filler={})",
                userId, suggestions.size(),
                Math.min(ranked.size(), capped),
                Math.max(0, suggestions.size() - Math.min(ranked.size(), capped)));
        return suggestions;
    }

    /**
     * Build the wire DTO for a suggestion entry. The {@code role} field is
     * synthesized from the follow-graph signal so the UI has something
     * honest to show ("Followed by N people you follow" or "Popular
     * traveler") instead of a hardcoded label.
     */
    private SuggestedUserDto buildSuggestionDto(User user, int mutualFollows, long followerCount) {
        SuggestedUserDto dto = new SuggestedUserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername() != null
                ? user.getUsername()
                : "user_" + user.getId().toString().substring(0, 8));
        dto.setName(user.getName() != null ? user.getName() : "Unknown User");
        dto.setProfilePictureUrl(user.getProfilePictureUrl());
        dto.setRole(describeSuggestion(mutualFollows, followerCount));
        dto.setIsFollowing(false);
        return dto;
    }

    private String describeSuggestion(int mutualFollows, long followerCount) {
        if (mutualFollows == 1) return "Followed by 1 person you follow";
        if (mutualFollows > 1) return "Followed by " + mutualFollows + " people you follow";
        if (followerCount >= 1000) return "Popular traveler";
        if (followerCount > 0) return "Traveler you might like";
        return "New on Travelo";
    }

    @Override
    public Map<String, Object> getUserStats(UUID userId) {
        logger.info("Getting user stats for user: {}", userId);
        
        // For now, return default values
        // TODO: Integrate with post-service to get actual counts
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("draft_count", 0L);
        stats.put("likes_and_saves_count", 0L);
        stats.put("ip_address", "India"); // TODO: Get actual IP location
        
        logger.info("Retrieved user stats for user {}", userId);
        return stats;
    }

    @Override
    public List<UserDto> listFollowers(UUID userId, UUID viewerId, int page, int limit) {
        int p = Math.max(page, 1);
        int lim = Math.min(Math.max(limit, 1), 100);
        Pageable pageable = PageRequest.of(p - 1, lim, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Follow> slice = followRepository.findByFolloweeId(userId, pageable);
        List<UserDto> out = new ArrayList<>();
        for (Follow f : slice.getContent()) {
            out.add(getUser(f.getFollowerId(), viewerId));
        }
        return out;
    }

    @Override
    public List<UserDto> listFollowing(UUID userId, UUID viewerId, int page, int limit) {
        int p = Math.max(page, 1);
        int lim = Math.min(Math.max(limit, 1), 100);
        Pageable pageable = PageRequest.of(p - 1, lim, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Follow> slice = followRepository.findByFollowerId(userId, pageable);
        List<UserDto> out = new ArrayList<>();
        for (Follow f : slice.getContent()) {
            out.add(getUser(f.getFolloweeId(), viewerId));
        }
        return out;
    }
}

