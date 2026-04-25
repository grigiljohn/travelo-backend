package com.travelo.mapservice.service;

import com.travelo.mapservice.dto.MapMediaResponseDto;

public interface MapQueryService {
    MapMediaResponseDto query(
            String userId,
            String mode,
            String bbox,
            double zoom,
            String mediaType,
            String collectionId,
            String tripId,
            String timeRange
    );
}
