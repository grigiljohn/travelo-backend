package com.travelo.feedservice.service;

import com.travelo.feedservice.client.dto.PostDto;

import java.util.List;
import java.util.UUID;

/**
 * Service for ranking feed items based on various signals.
 */
public interface FeedRankingService {
    
    /**
     * Rank posts based on multiple signals:
     * - Recency (time decay)
     * - User affinity (following relationships, past interactions)
     * - Post popularity (likes, comments, shares)
     * - Content type (reel bias)
     * - ML recommendations (future enhancement)
     * 
     * @param posts List of posts to rank
     * @param userId User ID for personalized ranking
     * @param followedUserIds List of user IDs that the current user follows
     * @return Ranked list of posts (highest score first)
     */
    List<PostDto> rankPosts(List<PostDto> posts, UUID userId, List<UUID> followedUserIds);
    
    /**
     * Calculate a single post's ranking score.
     */
    double calculateScore(PostDto post, UUID userId, boolean isFollowingAuthor);
}

