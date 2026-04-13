package com.travelo.adservice.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Request for ad delivery API.
 * Used by feed-service and reel-service to fetch ads for a specific placement.
 */
public record AdDeliveryRequest(
        String placement,  // "feed" or "reel"
        UUID userId,       // User to target ads for
        Map<String, Object> userContext  // Optional: location, device, etc.
) {
}

