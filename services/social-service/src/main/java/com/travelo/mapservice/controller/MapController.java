package com.travelo.mapservice.controller;

import com.travelo.commons.security.SecurityUtils;
import com.travelo.mapservice.dto.MapMediaResponseDto;
import com.travelo.mapservice.service.MapQueryService;
import com.travelo.postservice.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/map")
public class MapController {

    private final MapQueryService mapQueryService;

    public MapController(MapQueryService mapQueryService) {
        this.mapQueryService = mapQueryService;
    }

    @GetMapping("/media")
    public ResponseEntity<ApiResponse<MapMediaResponseDto>> media(
            @RequestParam("bbox") String bbox,
            @RequestParam("zoom") double zoom,
            @RequestParam(value = "mode", defaultValue = "collections") String mode,
            @RequestParam(value = "mediaType", required = false) String mediaType,
            @RequestParam(value = "collectionId", required = false) String collectionId,
            @RequestParam(value = "tripId", required = false) String tripId,
            @RequestParam(value = "timeRange", required = false) String timeRange
    ) {
        String userId = SecurityUtils.getCurrentUserIdAsString();
        if (userId == null || userId.isBlank()) {
            throw new SecurityException("User not authenticated");
        }
        MapMediaResponseDto dto = mapQueryService.query(
                userId, mode, bbox, zoom, mediaType, collectionId, tripId, timeRange
        );
        return ResponseEntity.ok(ApiResponse.success("Map media fetched", dto));
    }
}
