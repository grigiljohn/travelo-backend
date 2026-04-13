package com.travelo.searchservice.service;

import com.travelo.searchservice.document.PostDocument;
import com.travelo.searchservice.document.UserDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for filtering search results based on privacy rules.
 * 
 * Privacy Rules:
 * 1. Private Users: Only show if searcher is following them or is the user themselves
 * 2. Blocked Users: Never show if searcher has blocked them or they have blocked searcher
 * 3. Private Posts: Only show if post author is public, or searcher is following author, or searcher is author
 */
@Service
public class PrivacyFilterService {

    private static final Logger logger = LoggerFactory.getLogger(PrivacyFilterService.class);

    // TODO: Integrate with user-service to get blocked/following lists
    // For now, this is a placeholder that filters based on isPrivate flag only
    // In production, you would:
    // 1. Call user-service to get blocked users list
    // 2. Call follow-service to get following users list
    // 3. Cache these lists (Redis) to reduce service calls

    /**
     * Filter users based on privacy rules.
     * 
     * @param users List of user documents to filter
     * @param viewerId ID of the user performing the search (null if anonymous)
     * @return Filtered list of users
     */
    public List<UserDocument> filterUsers(List<UserDocument> users, String viewerId) {
        if (viewerId == null || viewerId.isEmpty()) {
            // Anonymous users: only show public users
            logger.debug("Filtering users for anonymous user: showing only public users");
            return users.stream()
                    .filter(user -> user.getIsPrivate() == null || !user.getIsPrivate())
                    .collect(Collectors.toList());
        }

        // TODO: Get blocked users and following users from user-service/follow-service
        // For now, we only filter based on isPrivate flag
        // If user is private and viewer is not the user themselves, hide them
        // In production, also check if viewer is following the user
        
        logger.debug("Filtering users for viewer: {}", viewerId);
        return users.stream()
                .filter(user -> {
                    // Skip users with null id (data integrity issue)
                    String userId = user.getId();
                    if (userId == null || userId.isEmpty()) {
                        logger.warn("User has null or empty id, skipping from results");
                        return false;
                    }
                    
                    // Show user if:
                    // 1. User is public, OR
                    // 2. Viewer is the user themselves, OR
                    // 3. Viewer is following the user (TODO: implement follow check)
                    boolean isPublic = user.getIsPrivate() == null || !user.getIsPrivate();
                    boolean isSelf = userId.equals(viewerId);
                    // TODO: boolean isFollowing = checkIfFollowing(viewerId, userId);
                    
                    return isPublic || isSelf; // || isFollowing;
                })
                .collect(Collectors.toList());
    }

    /**
     * Filter posts based on privacy rules.
     * 
     * @param posts List of post documents to filter
     * @param viewerId ID of the user performing the search (null if anonymous)
     * @param userPrivacyMap Map of userId -> isPrivate (to avoid N+1 queries)
     * @return Filtered list of posts
     */
    public List<PostDocument> filterPosts(List<PostDocument> posts, String viewerId, 
                                          java.util.Map<String, Boolean> userPrivacyMap) {
        if (viewerId == null || viewerId.isEmpty()) {
            // Anonymous users: only show posts from public users
            logger.debug("Filtering posts for anonymous user: showing only posts from public users");
            return posts.stream()
                    .filter(post -> {
                        // Skip posts with null userId (data integrity issue)
                        String postUserId = post.getUserId();
                        if (postUserId == null || postUserId.isEmpty()) {
                            logger.warn("Post {} has null or empty userId, skipping from results", post.getId());
                            return false;
                        }
                        Boolean isPrivate = userPrivacyMap.get(postUserId);
                        return isPrivate == null || !isPrivate;
                    })
                    .collect(Collectors.toList());
        }

        logger.debug("Filtering posts for viewer: {}", viewerId);
        return posts.stream()
                .filter(post -> {
                    // Skip posts with null userId (data integrity issue)
                    String postUserId = post.getUserId();
                    if (postUserId == null || postUserId.isEmpty()) {
                        logger.warn("Post {} has null or empty userId, skipping from results", post.getId());
                        return false;
                    }
                    
                    // Show post if:
                    // 1. Post author is public, OR
                    // 2. Viewer is the post author, OR
                    // 3. Viewer is following the post author (TODO: implement follow check)
                    Boolean isPrivate = userPrivacyMap.get(postUserId);
                    boolean isPublic = isPrivate == null || !isPrivate;
                    boolean isAuthor = postUserId.equals(viewerId);
                    // TODO: boolean isFollowing = checkIfFollowing(viewerId, postUserId);
                    
                    return isPublic || isAuthor; // || isFollowing;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get blocked user IDs for a viewer.
     * TODO: Implement by calling user-service API
     * 
     * @param viewerId ID of the viewer
     * @return Set of blocked user IDs
     */
    public Set<String> getBlockedUserIds(String viewerId) {
        // TODO: Call user-service: GET /api/v1/users/{viewerId}/blocked
        // For now, return empty set
        logger.debug("Getting blocked users for viewer: {} (placeholder - returns empty)", viewerId);
        return Set.of();
    }

    /**
     * Get following user IDs for a viewer.
     * TODO: Implement by calling follow-service API
     * 
     * @param viewerId ID of the viewer
     * @return Set of following user IDs
     */
    public Set<String> getFollowingUserIds(String viewerId) {
        // TODO: Call follow-service: GET /api/v1/users/{viewerId}/following
        // For now, return empty set
        logger.debug("Getting following users for viewer: {} (placeholder - returns empty)", viewerId);
        return Set.of();
    }
}

