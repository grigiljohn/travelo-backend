package com.travelo.reelservice.service;

import java.util.UUID;

/**
 * Service for reel likes.
 */
public interface ReelLikeService {
    
    /**
     * Like a reel.
     */
    void likeReel(UUID reelId, String userId);
    
    /**
     * Unlike a reel.
     */
    void unlikeReel(UUID reelId, String userId);
    
    /**
     * Check if user has liked a reel.
     */
    boolean hasUserLiked(UUID reelId, String userId);
}

