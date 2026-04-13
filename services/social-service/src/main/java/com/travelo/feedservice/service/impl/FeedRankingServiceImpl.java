package com.travelo.feedservice.service.impl;

import com.travelo.feedservice.client.dto.PostDto;
import com.travelo.feedservice.service.FeedRankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of feed ranking algorithm.
 * Uses multiple signals to rank posts:
 * - Recency (exponential decay)
 * - User affinity (following boost)
 * - Post popularity (engagement metrics)
 * - Content type (reel boost)
 */
@Service
public class FeedRankingServiceImpl implements FeedRankingService {

    private static final Logger logger = LoggerFactory.getLogger(FeedRankingServiceImpl.class);

    // Weight constants for ranking signals
    private static final double RECENCY_WEIGHT = 0.35;
    private static final double AFFINITY_WEIGHT = 0.30;
    private static final double POPULARITY_WEIGHT = 0.25;
    private static final double CONTENT_TYPE_WEIGHT = 0.10;

    // Following boost multiplier
    private static final double FOLLOWING_BOOST = 2.0;
    
    // Reel boost multiplier
    private static final double REEL_BOOST = 1.5;

    // Time decay half-life (in hours)
    private static final double HALF_LIFE_HOURS = 24.0;

    @Override
    public List<PostDto> rankPosts(List<PostDto> posts, UUID userId, List<UUID> followedUserIds) {
        if (posts.isEmpty()) {
            return posts;
        }

        logger.debug("Ranking {} posts for user {}", posts.size(), userId);

        Set<String> followedSet = followedUserIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toSet());

        // Calculate scores and create map
        Map<PostDto, Double> postScores = posts.stream()
                .collect(Collectors.toMap(
                        post -> post,
                        post -> calculateScore(post, userId, followedSet.contains(post.getUserId()))
                ));

        // Sort by score descending
        return posts.stream()
                .sorted((p1, p2) -> Double.compare(postScores.get(p2), postScores.get(p1)))
                .collect(Collectors.toList());
    }

    @Override
    public double calculateScore(PostDto post, UUID userId, boolean isFollowingAuthor) {
        double recencyScore = calculateRecencyScore(post);
        double affinityScore = calculateAffinityScore(isFollowingAuthor);
        double popularityScore = calculatePopularityScore(post);
        double contentTypeScore = calculateContentTypeScore(post);

        double finalScore = (recencyScore * RECENCY_WEIGHT) +
                           (affinityScore * AFFINITY_WEIGHT) +
                           (popularityScore * POPULARITY_WEIGHT) +
                           (contentTypeScore * CONTENT_TYPE_WEIGHT);

        logger.trace("Post {} score: recency={}, affinity={}, popularity={}, contentType={}, final={}",
                post.getId(), recencyScore, affinityScore, popularityScore, contentTypeScore, finalScore);

        return finalScore;
    }

    /**
     * Calculate recency score using exponential decay.
     * Score decreases as post gets older.
     */
    private double calculateRecencyScore(PostDto post) {
        if (post.getCreatedAt() == null) {
            return 0.5; // Default score for posts without timestamp
        }

        try {
            Instant postTime = Instant.parse(post.getCreatedAt());
            Instant now = Instant.now();
            long hoursAgo = ChronoUnit.HOURS.between(postTime, now);

            // Exponential decay: score = e^(-λ * t)
            // where λ = ln(2) / half_life
            double lambda = Math.log(2) / HALF_LIFE_HOURS;
            double score = Math.exp(-lambda * hoursAgo);

            // Normalize to [0, 1] range (clamp if needed)
            return Math.min(1.0, Math.max(0.0, score));
        } catch (Exception e) {
            logger.warn("Error parsing createdAt for post {}: {}", post.getId(), e.getMessage());
            return 0.5;
        }
    }

    /**
     * Calculate affinity score based on following relationship.
     */
    private double calculateAffinityScore(boolean isFollowingAuthor) {
        if (isFollowingAuthor) {
            return 1.0 * FOLLOWING_BOOST; // Boost for followed users
        }
        return 0.5; // Base score for non-followed users
    }

    /**
     * Calculate popularity score based on engagement metrics.
     */
    private double calculatePopularityScore(PostDto post) {
        // Normalize engagement metrics
        long likes = post.getLikes() != null ? post.getLikes() : 0;
        long comments = post.getComments() != null ? post.getComments() : 0;
        long shares = post.getShares() != null ? post.getShares() : 0;

        // Simple popularity formula: log(1 + engagement) / log(1 + max_engagement)
        // Using a soft max of 1000 engagements
        double engagement = likes + (comments * 2) + (shares * 3); // Weighted engagement
        double maxEngagement = 1000.0;

        // Logarithmic normalization to prevent extremely popular posts from dominating
        double score = Math.log(1 + engagement) / Math.log(1 + maxEngagement);

        // Normalize to [0, 1] range
        return Math.min(1.0, Math.max(0.0, score));
    }

    /**
     * Calculate content type score (reel boost).
     */
    private double calculateContentTypeScore(PostDto post) {
        String postType = post.getPostType() != null ? post.getPostType() : "";
        
        if ("reel".equalsIgnoreCase(postType) || "REEL".equalsIgnoreCase(postType)) {
            return 1.0 * REEL_BOOST; // Boost for reels
        }
        return 1.0; // Base score for other content types
    }
}

