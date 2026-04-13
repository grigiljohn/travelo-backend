package com.travelo.planservice.dto;

import java.util.List;

public record UnifiedSearchResponse(
        List<UnifiedSearchHit> users,
        List<UnifiedSearchHit> locations,
        List<UnifiedSearchHit> trips,
        List<UnifiedSearchHit> plans
) {
}
