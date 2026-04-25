package com.travelo.admin.api.catalog;

import java.util.Map;

/**
 * Public payload for the mobile app — mirrors a subset of the row plus
 * the JSON trip_preferences bag consumed as {@code TripPreferences.fromJson} on the client.
 */
public record PublicPredefinedTripDto(
        long id,
        String slug,
        String title,
        String subtitle,
        String heroImageUrl,
        Integer estimatedDays,
        Map<String, Object> tripPreferences
) {
}
