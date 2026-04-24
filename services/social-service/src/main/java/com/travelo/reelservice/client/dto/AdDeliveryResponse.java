package com.travelo.reelservice.client.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response for ad delivery API.
 */
public record AdDeliveryResponse(
        UUID adId,
        UUID adGroupId,
        UUID campaignId,
        String adType,  // Enum as string
        String format,  // "feed" or "reel-ad"
        
        // Creative content
        Map<String, Object> creative,  // Contains imageUrl, videoUrl, etc.
        List<String> headlines,
        List<String> descriptions,
        String callToAction,
        String ctaText,
        
        // URLs
        String finalUrl,
        String displayUrl,
        String brandName,
        String brandWebsite,

        UUID shopId,
        UUID productId,
        String shopUrl,
        
        // Metadata
        Map<String, Object> metadata
) {
}

