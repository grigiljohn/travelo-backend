package com.travelo.feedservice.service;

import java.util.Set;
import java.util.UUID;

/**
 * Service for managing seen posts per user per surface.
 * Uses Redis SET for O(1) lookups.
 */
public interface FeedSeenService {
    
    /**
     * Mark posts as seen for a user on a specific surface.
     * 
     * @param userId User ID
     * @param surface Surface name (e.g., "home", "explore", "reels")
     * @param postIds Set of post IDs to mark as seen
     */
    void markPostsAsSeen(UUID userId, String surface, Set<String> postIds);
    
    /**
     * Check which posts from a given set have been seen.
     * 
     * @param userId User ID
     * @param surface Surface name
     * @param postIds Post IDs to check
     * @return Set of post IDs that have been seen (subset of input)
     */
    Set<String> getSeenPostIds(UUID userId, String surface, Set<String> postIds);
    
    /**
     * Check if a specific post has been seen.
     * O(1) operation using Redis SISMEMBER.
     * 
     * @param userId User ID
     * @param surface Surface name
     * @param postId Post ID to check
     * @return true if seen, false otherwise
     */
    boolean isPostSeen(UUID userId, String surface, String postId);
    
    /**
     * Clear seen posts for a user on a specific surface (for testing/admin use).
     * 
     * @param userId User ID
     * @param surface Surface name
     */
    void clearSeenPosts(UUID userId, String surface);
}

