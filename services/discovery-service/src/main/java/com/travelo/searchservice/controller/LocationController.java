package com.travelo.searchservice.controller;

import com.travelo.searchservice.places.GooglePlacesService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/locations")
@Tag(name = "Locations", description = "Location lookup APIs for tagging")
public class LocationController {

    private static final Logger logger = LoggerFactory.getLogger(LocationController.class);

    private final GooglePlacesService googlePlacesService;

    public LocationController(GooglePlacesService googlePlacesService) {
        this.googlePlacesService = googlePlacesService;
    }

    @GetMapping
    @Operation(summary = "Search locations", description = "Uses Google Places when configured; otherwise demo suggestions")
    public ResponseEntity<Map<String, Object>> searchLocations(
            @RequestParam(value = "q", defaultValue = "") String query,
            @RequestParam(value = "limit", defaultValue = "12") int limit
    ) {
        final int safeLimit = Math.min(Math.max(limit, 1), 50);
        final String normalized = query.trim().toLowerCase();

        logger.info("GET /api/v1/locations - q='{}', limit={}", query, safeLimit);

        if (googlePlacesService.isConfigured() && query.trim().length() >= 2) {
            List<Map<String, Object>> rows = googlePlacesService
                    .autocompleteWithDetails(query.trim(), safeLimit)
                    .stream()
                    .map(googlePlacesService::toApiLocationRow)
                    .toList();
            if (!rows.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "query", query,
                        "count", rows.size(),
                        "data", rows
                ));
            }
        }

        final List<Map<String, Object>> filtered = filterHardcoded(normalized, safeLimit);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "query", query,
                "count", filtered.size(),
                "data", filtered
        ));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Nearby locations", description = "Uses Google Nearby Search when configured; otherwise demo pins")
    public ResponseEntity<Map<String, Object>> nearbyLocations(
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng,
            @RequestParam(value = "limit", defaultValue = "8") int limit
    ) {
        final int safeLimit = Math.min(Math.max(limit, 1), 50);
        logger.info("GET /api/v1/locations/nearby - lat={}, lng={}, limit={}", lat, lng, safeLimit);

        if (googlePlacesService.isConfigured()) {
            List<Map<String, Object>> rows = googlePlacesService
                    .nearbyPlaces(lat, lng, safeLimit)
                    .stream()
                    .map(googlePlacesService::toApiLocationRow)
                    .toList();
            if (!rows.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "count", rows.size(),
                        "data", rows
                ));
            }
        }

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

    @GetMapping("/details")
    @Operation(summary = "Place details", description = "Google Place Details by place_id (server key)")
    public ResponseEntity<Map<String, Object>> placeDetails(
            @RequestParam("place_id") String placeId
    ) {
        if (!googlePlacesService.isConfigured()) {
            return ResponseEntity.status(503).body(Map.of(
                    "success", false,
                    "message", "Google Places is not configured on the server"
            ));
        }
        String pid = placeId == null ? "" : placeId.trim();
        if (pid.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "place_id required"));
        }
        return googlePlacesService.fetchPlaceDetailsMap(pid)
                .map(data -> ResponseEntity.ok(Map.<String, Object>of("success", true, "data", data)))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "Place not found"
                )));
    }

    private static List<Map<String, Object>> filterHardcoded(String normalized, int safeLimit) {
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
        return filtered;
    }

    private static Map<String, Object> location(
            String id,
            String name,
            String subtitle,
            double lat,
            double lng,
            String type
    ) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("subtitle", subtitle);
        m.put("type", type);
        m.put("lat", lat);
        m.put("lng", lng);
        return m;
    }
}
