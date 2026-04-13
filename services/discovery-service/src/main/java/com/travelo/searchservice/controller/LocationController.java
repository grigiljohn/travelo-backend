package com.travelo.searchservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/locations")
@Tag(name = "Locations", description = "Location lookup APIs for tagging")
public class LocationController {

    private static final Logger logger = LoggerFactory.getLogger(LocationController.class);

    @GetMapping
    @Operation(summary = "Search locations", description = "Returns hardcoded location suggestions for tagging")
    public ResponseEntity<Map<String, Object>> searchLocations(
            @RequestParam(value = "q", defaultValue = "") String query,
            @RequestParam(value = "limit", defaultValue = "12") int limit
    ) {
        final int safeLimit = Math.min(Math.max(limit, 1), 50);
        final String normalized = query.trim().toLowerCase();

        logger.info("GET /api/v1/locations - q='{}', limit={}", query, safeLimit);

        final List<Map<String, Object>> all = List.of(
                location("loc_fort_kochi", "Fort Kochi Beach", "Kochi, India", 9.9652, 76.2428, "landmark"),
                location("loc_marine_drive_kochi", "Marine Drive", "Kochi, India", 9.9792, 76.2768, "waterfront"),
                location("loc_munnar_top_station", "Top Station", "Munnar, India", 10.1642, 77.2495, "mountain"),
                location("loc_goa_baga", "Baga Beach", "Goa, India", 15.5522, 73.7517, "beach"),
                location("loc_alleppey_boat", "Alleppey Backwaters", "Alappuzha, India", 9.4981, 76.3388, "waterfront"),
                location("loc_mysore_palace", "Mysore Palace", "Mysuru, India", 12.3051, 76.6551, "landmark"),
                location("loc_hampi", "Hampi Ruins", "Hampi, India", 15.3350, 76.4600, "heritage"),
                location("loc_manali_solang", "Solang Valley", "Manali, India", 32.3117, 77.1819, "mountain"),
                location("loc_ladakh_pangong", "Pangong Lake", "Ladakh, India", 33.7361, 78.9629, "lake"),
                location("loc_udaipur_city_palace", "City Palace", "Udaipur, India", 24.5760, 73.6834, "heritage"),
                location("loc_paris_louvre", "Louvre Museum", "Paris, France", 48.8606, 2.3376, "museum"),
                location("loc_tokyo_shibuya", "Shibuya Crossing", "Tokyo, Japan", 35.6595, 139.7005, "city")
        );

        final List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> item : all) {
            if (normalized.isBlank()) {
                filtered.add(item);
            } else {
                final String name = item.get("name").toString().toLowerCase();
                final String subtitle = item.get("subtitle").toString().toLowerCase();
                if (name.contains(normalized) || subtitle.contains(normalized)) {
                    filtered.add(item);
                }
            }
            if (filtered.size() >= safeLimit) {
                break;
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "query", query,
                "count", filtered.size(),
                "data", filtered
        ));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Nearby locations", description = "Returns hardcoded nearby locations for tagging")
    public ResponseEntity<Map<String, Object>> nearbyLocations(
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng,
            @RequestParam(value = "limit", defaultValue = "8") int limit
    ) {
        final int safeLimit = Math.min(Math.max(limit, 1), 50);
        logger.info("GET /api/v1/locations/nearby - lat={}, lng={}, limit={}", lat, lng, safeLimit);

        final List<Map<String, Object>> data = List.of(
                location("loc_near_1", "Sunset Point", "1.2 km away", lat + 0.0042, lng + 0.0031, "viewpoint"),
                location("loc_near_2", "Old Town Walk", "2.4 km away", lat + 0.0101, lng - 0.0022, "heritage"),
                location("loc_near_3", "Local Food Street", "0.9 km away", lat - 0.0039, lng + 0.0015, "food"),
                location("loc_near_4", "Riverside Promenade", "3.1 km away", lat + 0.0069, lng + 0.0080, "waterfront"),
                location("loc_near_5", "City Museum", "4.0 km away", lat - 0.0115, lng - 0.0040, "museum"),
                location("loc_near_6", "Night Market", "2.1 km away", lat + 0.0074, lng - 0.0054, "market")
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "count", Math.min(data.size(), safeLimit),
                "data", data.subList(0, Math.min(data.size(), safeLimit))
        ));
    }

    private Map<String, Object> location(
            String id,
            String name,
            String subtitle,
            double lat,
            double lng,
            String type
    ) {
        return Map.of(
                "id", id,
                "name", name,
                "subtitle", subtitle,
                "type", type,
                "lat", lat,
                "lng", lng
        );
    }
}
