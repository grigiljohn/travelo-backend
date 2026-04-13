package com.travelo.reelservice.service.impl;

import com.travelo.reelservice.client.AdServiceClient;
import com.travelo.reelservice.client.PostServiceClient;
import com.travelo.reelservice.client.dto.AdDeliveryResponse;
import com.travelo.reelservice.client.dto.PostDto;
import com.travelo.reelservice.dto.ReelItem;
import com.travelo.reelservice.service.ReelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of reel service that merges reels and ads.
 */
@Service
public class ReelServiceImpl implements ReelService {

    private static final Logger logger = LoggerFactory.getLogger(ReelServiceImpl.class);

    private final PostServiceClient postServiceClient;
    private final AdServiceClient adServiceClient;

    // Ad insertion configuration for reels
    private static final int MIN_REELS_BETWEEN_ADS = 3;
    private static final int MAX_REELS_BETWEEN_ADS = 6;

    public ReelServiceImpl(PostServiceClient postServiceClient, AdServiceClient adServiceClient) {
        this.postServiceClient = postServiceClient;
        this.adServiceClient = adServiceClient;
    }

    @Override
    public List<ReelItem> getReels(UUID userId, int page, int limit, String mood) {
        logger.info("Building reel feed for userId={}, page={}, limit={}, mood={}", userId, page, limit, mood);

        try {
            // Step 1: Fetch reels from post-service
            List<PostDto> reels;
            try {
                reels = postServiceClient.getReels(page, limit, mood);
                logger.debug("Fetched {} reels", reels.size());
            } catch (RuntimeException e) {
                logger.error("Failed to fetch reels from post-service for userId={}, page={}, limit={}: {}", 
                        userId, page, limit, e.getMessage(), e);
                // Re-throw to be handled by controller exception handler
                throw e;
            }

            if (reels.isEmpty()) {
                logger.info("No reels found, returning empty list");
                return Collections.emptyList();
            }

            // Step 2: Fetch reel ads from ad-service (non-blocking - ads are optional)
            // Request more ads than needed to avoid duplicates
            int adCount = calculateAdCount(reels.size());
            List<AdDeliveryResponse> ads = adServiceClient.fetchReelAds(userId, null, adCount * 2);
            logger.debug("Fetched {} reel ads", ads.size());

            // Step 3: Merge reels and ads with insertion logic
            return mergeReelsAndAds(reels, ads);
        } catch (RuntimeException e) {
            // Re-throw runtime exceptions (like PostServiceClient failures) to be handled by exception handler
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error building reel feed for userId={}, page={}, limit={}: {}", 
                    userId, page, limit, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch reels: " + e.getMessage(), e);
        }
    }

    /**
     * Calculate how many ads should be inserted based on reel count.
     */
    private int calculateAdCount(int reelCount) {
        if (reelCount <= MIN_REELS_BETWEEN_ADS) {
            return 0; // Not enough reels for an ad
        }
        // Rough estimate: one ad per (MIN + MAX) / 2 reels
        return (int) Math.ceil((double) reelCount / ((MIN_REELS_BETWEEN_ADS + MAX_REELS_BETWEEN_ADS) / 2.0));
    }

    /**
     * Merge reels and ads with insertion logic:
     * - Insert ad every 3-6 reels (randomized)
     * - Avoid duplicate ads consecutively
     * - Format ads as "reel-ad" for full-screen video ads
     */
    private List<ReelItem> mergeReelsAndAds(List<PostDto> reels, List<AdDeliveryResponse> ads) {
        if (ads.isEmpty()) {
            // No ads available, return only reels
            return reels.stream()
                    .map(ReelItem::fromReel)
                    .collect(Collectors.toList());
        }

        List<ReelItem> reelItems = new ArrayList<>();
        int adIndex = 0;
        int reelsSinceLastAd = 0;
        UUID lastAdId = null;
        Random random = new Random();

        for (PostDto reel : reels) {
            // Add the reel
            reelItems.add(ReelItem.fromReel(reel));
            reelsSinceLastAd++;

            // Check if we should insert an ad
            if (reelsSinceLastAd >= MIN_REELS_BETWEEN_ADS) {
                // Randomly decide if we should insert an ad (between MIN and MAX reels)
                int reelsUntilAd = MIN_REELS_BETWEEN_ADS + random.nextInt(MAX_REELS_BETWEEN_ADS - MIN_REELS_BETWEEN_ADS + 1);
                
                if (reelsSinceLastAd >= reelsUntilAd && adIndex < ads.size()) {
                    // Find next ad that's different from the last one
                    AdDeliveryResponse adToInsert = findNextNonDuplicateAd(ads, lastAdId, adIndex);
                    
                    if (adToInsert != null) {
                        ReelItem adItem = ReelItem.fromAd(adToInsert);
                        reelItems.add(adItem);
                        lastAdId = adToInsert.adId();
                        adIndex = ads.indexOf(adToInsert) + 1;
                        reelsSinceLastAd = 0; // Reset counter
                        logger.debug("Inserted reel ad: {}", adToInsert.adId());
                    }
                }
            }
        }

        logger.info("Created reel feed with {} reels and {} ads", 
                reelItems.stream().filter(item -> "reel".equals(item.getType())).count(),
                reelItems.stream().filter(item -> "ad".equals(item.getType())).count());

        return reelItems;
    }

    /**
     * Find next ad that's different from the last inserted ad to avoid duplicates.
     */
    private AdDeliveryResponse findNextNonDuplicateAd(List<AdDeliveryResponse> ads, UUID lastAdId, int startIndex) {
        // If no last ad, return first available
        if (lastAdId == null) {
            return startIndex < ads.size() ? ads.get(startIndex) : null;
        }

        // Find next ad that's different from last
        for (int i = startIndex; i < ads.size(); i++) {
            AdDeliveryResponse ad = ads.get(i);
            if (!lastAdId.equals(ad.adId())) {
                return ad;
            }
        }

        // If all remaining ads are duplicates, cycle back to beginning (avoid if possible)
        if (ads.size() > 1) {
            for (int i = 0; i < startIndex && i < ads.size(); i++) {
                AdDeliveryResponse ad = ads.get(i);
                if (!lastAdId.equals(ad.adId())) {
                    return ad;
                }
            }
        }

        // If we only have one ad or all are duplicates, return null (skip ad insertion)
        return null;
    }
}

