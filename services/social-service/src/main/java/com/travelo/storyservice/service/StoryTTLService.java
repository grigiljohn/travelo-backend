package com.travelo.storyservice.service;

import java.util.UUID;

/**
 * Service for managing story TTL in Redis.
 */
public interface StoryTTLService {
    
    /**
     * Set TTL for a story in Redis (24 hours).
     * Key format: "story:ttl:{storyId}"
     */
    void setStoryTTL(UUID storyId, long ttlSeconds);
    
    /**
     * Check if a story exists and is not expired in Redis.
     */
    boolean isStoryActive(UUID storyId);
    
    /**
     * Remove story from Redis (when expired or deleted).
     */
    void removeStoryTTL(UUID storyId);
    
    /**
     * Get remaining TTL for a story in seconds.
     */
    long getRemainingTTL(UUID storyId);
}

