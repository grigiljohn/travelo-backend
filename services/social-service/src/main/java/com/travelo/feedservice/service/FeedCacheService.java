package com.travelo.feedservice.service;

import com.travelo.feedservice.dto.FeedItem;

import java.util.List;
import java.util.UUID;

/**
 * Service for caching feed in Redis using sorted sets.
 * Uses Redis sorted sets to store ranked feed items with scores.
 */
public interface FeedCacheService {
    
    /**
     * Get feed from cache (Redis).
     * @param userId User ID
     * @param cursor Cursor for pagination (Redis score-based)
     * @param limit Number of items to retrieve
     * @return List of feed items
     */
    List<FeedItem> getCachedFeed(UUID userId, String cursor, int limit);
    
    /**
     * Cache a ranked feed in Redis.
     * Uses sorted set with score = ranking score (higher is better).
     * @param userId User ID
     * @param feedItems Ranked feed items
     */
    void cacheFeed(UUID userId, List<FeedItem> feedItems);
    
    /**
     * Invalidate feed cache for a user.
     * @param userId User ID
     */
    void invalidateFeed(UUID userId);
    
    /**
     * Check if feed is cached for a user.
     * @param userId User ID
     * @return true if cached, false otherwise
     */
    boolean isFeedCached(UUID userId);
    
    /**
     * Get total cached feed count for a user.
     * @param userId User ID
     * @return Number of items in cache
     */
    long getCachedFeedCount(UUID userId);
}

