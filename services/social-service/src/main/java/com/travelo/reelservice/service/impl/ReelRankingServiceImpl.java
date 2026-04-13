package com.travelo.reelservice.service.impl;

import com.travelo.reelservice.entity.Reel;
import com.travelo.reelservice.repository.ReelRepository;
import com.travelo.reelservice.repository.ReelViewRepository;
import com.travelo.reelservice.service.ReelRankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReelRankingServiceImpl implements ReelRankingService {

    private static final Logger logger = LoggerFactory.getLogger(ReelRankingServiceImpl.class);

    // Weight constants for ranking signals
    private static final double COMPLETION_RATE_WEIGHT = 0.30;
    private static final double ENGAGEMENT_VELOCITY_WEIGHT = 0.25;
    private static final double RECENCY_WEIGHT = 0.20;
    private static final double RECOMMENDATION_SCORE_WEIGHT = 0.15;
    private static final double INTERACTION_PATTERN_WEIGHT = 0.10;

    // Time decay half-life (in hours)
    private static final double HALF_LIFE_HOURS = 24.0;

    private final ReelRepository reelRepository;
    private final ReelViewRepository reelViewRepository;

    public ReelRankingServiceImpl(ReelRepository reelRepository, ReelViewRepository reelViewRepository) {
        this.reelRepository = reelRepository;
        this.reelViewRepository = reelViewRepository;
    }

    @Override
    public double calculateRankingScore(Reel reel) {
        double completionRate = calculateCompletionRate(reel);
        double engagementVelocity = calculateEngagementVelocity(reel);
        double recency = calculateRecency(reel);
        double recommendationScore = reel.getRecommendationScore() != null ? reel.getRecommendationScore() : 0.0;
        double interactionPattern = 0.5; // Placeholder - would use user interaction history

        double finalScore = (completionRate * COMPLETION_RATE_WEIGHT) +
                           (engagementVelocity * ENGAGEMENT_VELOCITY_WEIGHT) +
                           (recency * RECENCY_WEIGHT) +
                           (recommendationScore * RECOMMENDATION_SCORE_WEIGHT) +
                           (interactionPattern * INTERACTION_PATTERN_WEIGHT);

        logger.trace("Reel {} score: completion={}, engagement={}, recency={}, recommendation={}, final={}",
                reel.getId(), completionRate, engagementVelocity, recency, recommendationScore, finalScore);

        return finalScore;
    }

    @Override
    public List<Reel> rankReels(List<Reel> reels) {
        return reels.stream()
                .sorted(Comparator
                        .comparing((Reel r) -> r.getRankingScore() != null ? r.getRankingScore() : 0.0)
                        .reversed()
                        .thenComparing(Reel::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(transactionManager = "reelTransactionManager")
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void updateRankingScores() {
        logger.info("Starting batch ranking score update");
        
        List<Reel> reels = reelRepository.findByStatus(Reel.Status.READY);
        
        for (Reel reel : reels) {
            double score = calculateRankingScore(reel);
            reel.setRankingScore(score);
            reelRepository.save(reel);
        }
        
        logger.info("Updated ranking scores for {} reels", reels.size());
    }

    private double calculateCompletionRate(Reel reel) {
        Double avgCompletion = reelViewRepository.getAverageCompletionPercentage(reel.getId());
        if (avgCompletion == null || avgCompletion == 0) {
            return 0.5; // Default if no views
        }
        return avgCompletion / 100.0; // Normalize to 0-1
    }

    private double calculateEngagementVelocity(Reel reel) {
        if (reel.getCreatedAt() == null) {
            return 0.5;
        }

        OffsetDateTime now = OffsetDateTime.now();
        long hoursAgo = Duration.between(reel.getCreatedAt(), now).toHours();
        
        if (hoursAgo == 0) {
            hoursAgo = 1; // Avoid division by zero
        }

        double engagement = reel.getLikeCount() + (reel.getCommentCount() * 1.5);
        double velocity = engagement / (double) hoursAgo;

        // Normalize using logarithmic scale (max velocity = 100 engagements/hour)
        double normalized = Math.log(1 + velocity) / Math.log(1 + 100.0);
        return Math.min(1.0, Math.max(0.0, normalized));
    }

    private double calculateRecency(Reel reel) {
        if (reel.getCreatedAt() == null) {
            return 0.5;
        }

        OffsetDateTime now = OffsetDateTime.now();
        long hoursAgo = Duration.between(reel.getCreatedAt(), now).toHours();

        // Exponential decay
        double lambda = Math.log(2) / HALF_LIFE_HOURS;
        double score = Math.exp(-lambda * hoursAgo);

        return Math.min(1.0, Math.max(0.0, score));
    }
}

