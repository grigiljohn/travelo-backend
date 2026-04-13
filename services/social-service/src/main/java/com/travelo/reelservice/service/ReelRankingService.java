package com.travelo.reelservice.service;

import com.travelo.reelservice.entity.Reel;
import java.util.List;

/**
 * Service for ranking reels with separate algorithm from posts.
 */
public interface ReelRankingService {
    
    /**
     * Calculate ranking score for a reel.
     * Uses different signals than post ranking:
     * - View completion rate
     * - Engagement velocity
     * - Recency
     * - User interaction patterns
     */
    double calculateRankingScore(Reel reel);
    
    /**
     * Rank a list of reels by their scores.
     */
    List<Reel> rankReels(List<Reel> reels);
    
    /**
     * Update ranking scores for all reels (batch job).
     */
    void updateRankingScores();
}

