package com.travelo.feedservice.client.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Request for ad delivery API.
 */
public record AdDeliveryRequest(
        String placement,  // "feed" or "reel"
        UUID userId,       // User to target ads for
        Map<String, Object> userContext  // Optional: location, device, etc.
) {
}

