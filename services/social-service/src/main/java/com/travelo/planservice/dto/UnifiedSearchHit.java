package com.travelo.planservice.dto;

import java.util.Map;

/**
 * Single row in unified travel search (users, curated locations, trips, plans).
 */
public record UnifiedSearchHit(
        String id,
        String type,
        String title,
        String subtitle,
        String imageUrl,
        Map<String, Object> metadata
) {
}
