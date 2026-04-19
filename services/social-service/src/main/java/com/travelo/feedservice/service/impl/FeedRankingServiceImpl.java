package com.travelo.feedservice.service.impl;

import com.travelo.feedservice.client.dto.PostDto;
import com.travelo.feedservice.dto.FeedRankingDebugItem;
import com.travelo.feedservice.service.FeedRankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    // Production-oriented defaults, override via application.yml/env.
    private final double recencyWeight;
    private final double affinityWeight;
    private final double popularityWeight;
    private final double contentTypeWeight;
    private final double followingBoost;
    private final double reelBoost;
    private final double nonFollowingAffinityScore;
    private final double halfLifeHours;
    private final int maxConsecutiveSameAuthor;
    private final int maxConsecutiveReels;

    public FeedRankingServiceImpl(
            @Value("${app.feed.ranking.weights.recency:0.35}") double recencyWeight,
            @Value("${app.feed.ranking.weights.affinity:0.30}") double affinityWeight,
            @Value("${app.feed.ranking.weights.popularity:0.25}") double popularityWeight,
            @Value("${app.feed.ranking.weights.content-type:0.10}") double contentTypeWeight,
            @Value("${app.feed.ranking.following-boost:2.0}") double followingBoost,
            @Value("${app.feed.ranking.reel-boost:1.5}") double reelBoost,
            @Value("${app.feed.ranking.non-following-affinity-score:0.5}") double nonFollowingAffinityScore,
            @Value("${app.feed.ranking.recency-half-life-hours:24.0}") double halfLifeHours,
            @Value("${app.feed.diversity.max-consecutive-same-author:2}") int maxConsecutiveSameAuthor,
            @Value("${app.feed.diversity.max-consecutive-reels:2}") int maxConsecutiveReels) {
        this.recencyWeight = recencyWeight;
        this.affinityWeight = affinityWeight;
        this.popularityWeight = popularityWeight;
        this.contentTypeWeight = contentTypeWeight;
        this.followingBoost = followingBoost;
        this.reelBoost = reelBoost;
        this.nonFollowingAffinityScore = nonFollowingAffinityScore;
        this.halfLifeHours = halfLifeHours;
        this.maxConsecutiveSameAuthor = Math.max(1, maxConsecutiveSameAuthor);
        this.maxConsecutiveReels = Math.max(1, maxConsecutiveReels);
    }

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

        // Sort by score descending then apply diversity pass.
        List<PostDto> sortedByScore = posts.stream()
                .sorted((p1, p2) -> Double.compare(postScores.get(p2), postScores.get(p1)))
                .collect(Collectors.toList());
        return applyDiversityConstraints(sortedByScore, postScores);
    }

    @Override
    public double calculateScore(PostDto post, UUID userId, boolean isFollowingAuthor) {
        double recencyScore = calculateRecencyScore(post);
        double affinityScore = calculateAffinityScore(isFollowingAuthor);
        double popularityScore = calculatePopularityScore(post);
        double contentTypeScore = calculateContentTypeScore(post);

        double finalScore = (recencyScore * recencyWeight) +
                           (affinityScore * affinityWeight) +
                           (popularityScore * popularityWeight) +
                           (contentTypeScore * contentTypeWeight);

        logger.trace("Post {} score: recency={}, affinity={}, popularity={}, contentType={}, final={}",
                post.getId(), recencyScore, affinityScore, popularityScore, contentTypeScore, finalScore);

        return finalScore;
    }

    @Override
    public FeedRankingDebugItem buildDebugItem(PostDto post, UUID userId, boolean isFollowingAuthor) {
        FeedRankingDebugItem debug = new FeedRankingDebugItem();
        debug.setPostId(post != null ? post.getId() : null);
        debug.setAuthorUserId(post != null ? post.getUserId() : null);
        debug.setPostType(post != null ? post.getPostType() : null);
        debug.setFollowingAuthor(isFollowingAuthor);

        double recency = calculateRecencyScore(post);
        double affinity = calculateAffinityScore(isFollowingAuthor);
        double popularity = calculatePopularityScore(post);
        double contentType = calculateContentTypeScore(post);
        double baseScore = (recency * recencyWeight)
                + (affinity * affinityWeight)
                + (popularity * popularityWeight)
                + (contentType * contentTypeWeight);

        debug.setRecencyComponent(recency * recencyWeight);
        debug.setAffinityComponent(affinity * affinityWeight);
        debug.setPopularityComponent(popularity * popularityWeight);
        debug.setContentTypeComponent(contentType * contentTypeWeight);
        debug.setBaseScore(baseScore);
        debug.setOnlineSignalScore(0.0d);
        debug.setFinalScore(baseScore);
        return debug;
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
            double lambda = Math.log(2) / halfLifeHours;
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
            return followingBoost; // Boost for followed users
        }
        return nonFollowingAffinityScore; // Base score for non-followed users
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
            return reelBoost; // Boost for reels
        }
        return 1.0; // Base score for other content types
    }

    /**
     * Greedy diversity reranker:
     * - avoids long runs from same author
     * - avoids long runs of reels
     * while staying as close as possible to score ordering.
     */
    private List<PostDto> applyDiversityConstraints(List<PostDto> sortedByScore, Map<PostDto, Double> scoreByPost) {
        if (sortedByScore.isEmpty()) {
            return sortedByScore;
        }
        List<PostDto> remaining = new ArrayList<>(sortedByScore);
        List<PostDto> result = new ArrayList<>(sortedByScore.size());

        int sameAuthorStreak = 0;
        int reelStreak = 0;
        String lastAuthorId = null;

        while (!remaining.isEmpty()) {
            PostDto best = pickNextEligible(
                    remaining,
                    scoreByPost,
                    lastAuthorId,
                    sameAuthorStreak,
                    reelStreak
            );
            result.add(best);
            remaining.remove(best);

            String currentAuthorId = best.getUserId();
            boolean isReel = "reel".equalsIgnoreCase(best.getPostType());
            sameAuthorStreak = Objects.equals(currentAuthorId, lastAuthorId) ? (sameAuthorStreak + 1) : 1;
            reelStreak = isReel ? (reelStreak + 1) : 0;
            lastAuthorId = currentAuthorId;
        }
        return result;
    }

    private PostDto pickNextEligible(
            List<PostDto> remaining,
            Map<PostDto, Double> scoreByPost,
            String lastAuthorId,
            int sameAuthorStreak,
            int reelStreak) {
        for (PostDto candidate : remaining) {
            if (isCandidateAllowed(candidate, lastAuthorId, sameAuthorStreak, reelStreak)) {
                return candidate;
            }
        }
        // All candidates violate constraints: fail-open and keep highest score.
        return remaining.stream()
                .max(Comparator.comparingDouble(p -> scoreByPost.getOrDefault(p, 0.0)))
                .orElse(remaining.get(0));
    }

    private boolean isCandidateAllowed(PostDto candidate, String lastAuthorId, int sameAuthorStreak, int reelStreak) {
        boolean candidateIsReel = "reel".equalsIgnoreCase(candidate.getPostType());
        boolean sameAuthor = Objects.equals(candidate.getUserId(), lastAuthorId);
        if (sameAuthor && sameAuthorStreak >= maxConsecutiveSameAuthor) {
            return false;
        }
        if (candidateIsReel && reelStreak >= maxConsecutiveReels) {
            return false;
        }
        return true;
    }
}

