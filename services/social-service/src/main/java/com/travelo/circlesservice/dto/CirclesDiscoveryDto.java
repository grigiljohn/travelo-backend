package com.travelo.circlesservice.dto;

import java.util.List;

/**
 * Payload for {@code GET /api/v1/circles/discovery} — location summary + nearby people.
 */
public record CirclesDiscoveryDto(
        String city,
        int nearbyTravelerCount,
        List<NearTravelerDto> people
) {
}
