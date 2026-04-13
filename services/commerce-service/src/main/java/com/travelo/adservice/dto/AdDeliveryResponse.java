package com.travelo.adservice.dto;

import com.travelo.adservice.entity.enums.AdType;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response for ad delivery API.
 * Contains ad information needed for display in feed/reels.
 */
public record AdDeliveryResponse(
        UUID adId,
        UUID adGroupId,
        UUID campaignId,
        AdType adType,
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
        
        // Shop routing (for SHOP_NOW CTA)
        UUID shopId,
        UUID productId,
        String shopUrl,  // Internal app URL: "travelo://shop/{shopId}"
        
        // Metadata
        Map<String, Object> metadata
) {
    public static AdDeliveryResponse fromAdResponse(AdResponse adResponse, String format, Map<String, Object> metadata) {
        return new AdDeliveryResponse(
                adResponse.getId(),
                adResponse.getAdGroupId(),
                null,  // campaignId - can be added if needed
                adResponse.getAdType(),
                format,
                null,  // creative - should be populated from Asset/Creative
                adResponse.getHeadlines() != null ? adResponse.getHeadlines() : List.of(),
                adResponse.getDescriptions() != null ? adResponse.getDescriptions() : List.of(),
                adResponse.getCallToAction(),
                null,  // ctaText - can be extracted from creative
                adResponse.getFinalUrl(),
                adResponse.getDisplayUrl(),
                null,  // brandName - from campaign/ad
                null,  // brandWebsite - from campaign/ad
                null,  // shopId
                null,  // productId
                null,  // shopUrl
                metadata != null ? metadata : Map.of()
        );
    }
}

