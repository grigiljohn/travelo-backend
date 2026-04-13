package com.travelo.feedservice.service;

import com.travelo.feedservice.dto.FeedResponse;

import java.util.UUID;

/**
 * Feed service interface for generating personalized feeds.
 */
public interface FeedService {
    
    /**
     * Get personalized feed with cursor-based pagination and seen filtering.
     * 
     * @param userId User ID
     * @param cursor Cursor for pagination (optional)
     * @param limit Number of items to retrieve
     * @param mood Optional mood filter
     * @param surface Feed surface (e.g., "home", "explore", "reels") for seen filtering
     * @return Feed response with items and next cursor (excluding seen posts)
     */
    FeedResponse getFeed(UUID userId, String cursor, int limit, String mood, String surface);
    
    /**
     * Refresh/regenerate feed for a user.
     * This invalidates cache and triggers feed recomputation.
     * 
     * @param userId User ID
     */
    void refreshFeed(UUID userId);
    
    /**
     * Precompute and cache feed for a user (fan-out on write).
     * Called when a followed user creates a new post.
     * 
     * @param userId User ID to update feed for
     * @param postId New post ID to add to feed
     */
    void addPostToFeed(UUID userId, UUID postId);
}
