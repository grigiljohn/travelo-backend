package com.travelo.postservice.controller;

import com.travelo.postservice.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for destination-related endpoints.
 * Returns mock data for development.
 * TODO: Replace with actual destination-service integration
 */
@RestController
@RequestMapping("/api/v1/destinations")
public class DestinationController {

    private static final Logger logger = LoggerFactory.getLogger(DestinationController.class);

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDestinations(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "search", required = false) String search) {
        logger.info("Getting destinations with category: {}, search: {}", category, search);
        
        // Return mock data
        List<Map<String, Object>> destinations = getMockDestinations(category, search);
        return ResponseEntity.ok(ApiResponse.success("Destinations retrieved successfully", destinations));
    }

    @GetMapping("/{destinationId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDestination(
            @PathVariable String destinationId) {
        logger.info("Getting destination {}", destinationId);
        
        // Return mock data
        Map<String, Object> destination = getMockDestination(destinationId);
        return ResponseEntity.ok(ApiResponse.success("Destination retrieved successfully", destination));
    }

    private List<Map<String, Object>> getMockDestinations(String category, String search) {
        List<Map<String, Object>> allDestinations = new ArrayList<>(List.of(
            Map.of(
                "id", "dest-1",
                "title", "Santorini Sunset View",
                "location", "Oia, Greece",
                "imageUrl", "https://picsum.photos/400/300?random=1",
                "price", "$150/night",
                "rating", "4.8"
            ),
            Map.of(
                "id", "dest-2",
                "title", "Tokyo City Lights",
                "location", "Shibuya, Japan",
                "imageUrl", "https://picsum.photos/400/300?random=2",
                "price", "$120/night",
                "rating", "4.9"
            ),
            Map.of(
                "id", "dest-3",
                "title", "Paris Romantic Getaway",
                "location", "Montmartre, France",
                "imageUrl", "https://picsum.photos/400/300?random=3",
                "price", "$180/night",
                "rating", "4.7"
            ),
            Map.of(
                "id", "dest-4",
                "title", "Bali Beach Paradise",
                "location", "Ubud, Indonesia",
                "imageUrl", "https://picsum.photos/400/300?random=4",
                "price", "$90/night",
                "rating", "4.6"
            ),
            Map.of(
                "id", "dest-5",
                "title", "Swiss Alpine Retreat",
                "location", "Interlaken, Switzerland",
                "imageUrl", "https://picsum.photos/400/300?random=5",
                "price", "$200/night",
                "rating", "4.9"
            ),
            Map.of(
                "id", "dest-6",
                "title", "Dubai Luxury Experience",
                "location", "Dubai Marina, UAE",
                "imageUrl", "https://picsum.photos/400/300?random=6",
                "price", "$250/night",
                "rating", "4.8"
            )
        ));

        // Filter by category if provided
        if (category != null && !category.equals("All")) {
            // Simple category filtering (in real implementation, would filter by actual category)
            return allDestinations;
        }

        // Filter by search query if provided
        if (search != null && !search.isEmpty()) {
            String lowerSearch = search.toLowerCase();
            return allDestinations.stream()
                    .filter(dest -> {
                        String title = (String) dest.get("title");
                        String location = (String) dest.get("location");
                        return title.toLowerCase().contains(lowerSearch) ||
                               location.toLowerCase().contains(lowerSearch);
                    })
                    .collect(Collectors.toList());
        }

        return allDestinations;
    }

    private Map<String, Object> getMockDestination(String destinationId) {
        return Map.of(
            "id", destinationId,
            "title", "Santorini Sunset View",
            "location", "Oia, Greece",
            "imageUrl", "https://picsum.photos/400/300?random=1",
            "price", "$150/night",
            "rating", "4.8",
            "description", "Beautiful villa with stunning sunset views over the Aegean Sea",
            "amenities", List.of("WiFi", "Pool", "Kitchen", "Parking"),
            "reviews", 124
        );
    }
}

