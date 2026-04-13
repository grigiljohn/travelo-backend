package com.travelo.circlesservice.dto;

import java.util.List;

/**
 * Traveler card for Circles "People around you" (mobile {@code CircleUser}).
 */
public record NearTravelerDto(
        String id,
        String name,
        String avatarUrl,
        String tag,
        List<String> interests,
        String distanceLabel,
        String bio,
        int tripsCount
) {
}
