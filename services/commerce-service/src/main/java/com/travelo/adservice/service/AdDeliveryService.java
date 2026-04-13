package com.travelo.adservice.service;

import com.travelo.adservice.dto.AdDeliveryRequest;
import com.travelo.adservice.dto.AdDeliveryResponse;

import java.util.List;

/**
 * Service for ad delivery to feed-service and reel-service.
 * Handles ad selection based on placement, targeting, and frequency caps.
 */
public interface AdDeliveryService {
    
    /**
     * Fetch ads for a specific placement (feed or reel) with targeting.
     * 
     * @param request Ad delivery request with placement and user context
     * @param count Number of ads to return
     * @return List of ads ready for insertion into feed/reels
     */
    List<AdDeliveryResponse> fetchAdsForPlacement(AdDeliveryRequest request, int count);
}

