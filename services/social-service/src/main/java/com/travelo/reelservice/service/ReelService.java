package com.travelo.reelservice.service;

import com.travelo.reelservice.dto.ReelItem;

import java.util.List;
import java.util.UUID;

/**
 * Service for building reel feed with reels and ads.
 */
public interface ReelService {
    
    /**
     * Get reel items with ads inserted.
     * 
     * @param userId User ID for targeting
     * @param page Page number
     * @param limit Page size
     * @param mood Optional mood filter
     * @return List of reel items (reels and ads)
     */
    List<ReelItem> getReels(UUID userId, int page, int limit, String mood);
}

