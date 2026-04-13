package com.travelo.reelservice.service.impl;

import com.travelo.reelservice.client.AnalyticsServiceClient;
import com.travelo.reelservice.entity.Reel;
import com.travelo.reelservice.entity.ReelLike;
import com.travelo.reelservice.exception.ReelNotFoundException;
import com.travelo.reelservice.repository.ReelLikeRepository;
import com.travelo.reelservice.repository.ReelRepository;
import com.travelo.reelservice.service.ReelLikeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(transactionManager = "reelTransactionManager")
public class ReelLikeServiceImpl implements ReelLikeService {

    private static final Logger logger = LoggerFactory.getLogger(ReelLikeServiceImpl.class);

    private final ReelRepository reelRepository;
    private final ReelLikeRepository reelLikeRepository;
    private final AnalyticsServiceClient analyticsServiceClient;

    public ReelLikeServiceImpl(ReelRepository reelRepository,
                               ReelLikeRepository reelLikeRepository,
                               AnalyticsServiceClient analyticsServiceClient) {
        this.reelRepository = reelRepository;
        this.reelLikeRepository = reelLikeRepository;
        this.analyticsServiceClient = analyticsServiceClient;
    }

    @Override
    public void likeReel(UUID reelId, String userId) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ReelNotFoundException(reelId));

        Optional<ReelLike> existingLike = reelLikeRepository.findByReelIdAndUserId(reelId, userId);
        if (existingLike.isPresent()) {
            logger.debug("User {} already liked reel {}", userId, reelId);
            return;
        }

        ReelLike like = new ReelLike(reelId, userId);
        reelLikeRepository.save(like);

        // Update like count
        reel.setLikeCount(reel.getLikeCount() + 1);
        reelRepository.save(reel);

        // Track analytics event
        analyticsServiceClient.trackReelLike(reelId, userId);

        logger.info("User {} liked reel {}", userId, reelId);
    }

    @Override
    public void unlikeReel(UUID reelId, String userId) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ReelNotFoundException(reelId));

        Optional<ReelLike> existingLike = reelLikeRepository.findByReelIdAndUserId(reelId, userId);
        if (existingLike.isEmpty()) {
            logger.debug("User {} has not liked reel {}", userId, reelId);
            return;
        }

        reelLikeRepository.delete(existingLike.get());

        // Update like count
        reel.setLikeCount(Math.max(0, reel.getLikeCount() - 1));
        reelRepository.save(reel);

        logger.info("User {} unliked reel {}", userId, reelId);
    }

    @Override
    public boolean hasUserLiked(UUID reelId, String userId) {
        return reelLikeRepository.existsByReelIdAndUserId(reelId, userId);
    }
}

