package com.travelo.postservice.controller;

import com.travelo.postservice.dto.ApiResponse;
import com.travelo.postservice.dto.LocationDto;
import com.travelo.postservice.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {
    private static final Logger logger = LoggerFactory.getLogger(LocationController.class);
    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<LocationDto>>> searchLocations(@RequestParam String q) {
        try {
            List<LocationDto> locations = locationService.searchLocations(q);
            return ResponseEntity.ok(ApiResponse.success("Locations found", locations));
        } catch (Exception e) {
            logger.error("Error searching locations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to search locations: " + e.getMessage(), "LOCATION_SEARCH_FAILED"));
        }
    }
}

