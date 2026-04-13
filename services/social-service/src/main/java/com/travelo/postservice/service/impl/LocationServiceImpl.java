package com.travelo.postservice.service.impl;

import com.travelo.postservice.dto.LocationDto;
import com.travelo.postservice.entity.Location;
import com.travelo.postservice.repository.LocationRepository;
import com.travelo.postservice.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocationServiceImpl implements LocationService {
    private static final Logger logger = LoggerFactory.getLogger(LocationServiceImpl.class);
    
    private final LocationRepository locationRepository;
    
    public LocationServiceImpl(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }
    
    @Override
    public List<LocationDto> searchLocations(String query) {
        logger.info("Searching locations with query: {}", query);
        
        // TODO: Replace with Google Places API integration
        // For now, return dummy data based on query
        List<LocationDto> dummyLocations = generateDummyLocations(query);
        logger.info("Returning {} dummy locations for query: {}", dummyLocations.size(), query);
        return dummyLocations;
    }
    
    /**
     * Generate dummy location data based on query.
     * This will be replaced with Google Places API integration later.
     */
    private List<LocationDto> generateDummyLocations(String query) {
        List<LocationDto> locations = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        
        // Popular travel destinations
        if (lowerQuery.contains("paris") || lowerQuery.contains("france")) {
            locations.add(new LocationDto(
                "loc-001",
                "Paris, France",
                "Paris, Île-de-France, France",
                "Paris, France",
                new BigDecimal("48.8566"),
                new BigDecimal("2.3522"),
                "ChIJLU7jZClu5kcR4PcOOO6p3I0",
                "FR",
                "Paris"
            ));
            locations.add(new LocationDto(
                "loc-002",
                "Eiffel Tower",
                "Eiffel Tower, Paris, France",
                "Champ de Mars, 5 Avenue Anatole France, 75007 Paris, France",
                new BigDecimal("48.8584"),
                new BigDecimal("2.2945"),
                "ChIJD7fiBh9u5kcRYJSMaMOCCwQ",
                "FR",
                "Paris"
            ));
        }
        
        if (lowerQuery.contains("tokyo") || lowerQuery.contains("japan")) {
            locations.add(new LocationDto(
                "loc-003",
                "Tokyo, Japan",
                "Tokyo, Japan",
                "Tokyo, Japan",
                new BigDecimal("35.6762"),
                new BigDecimal("139.6503"),
                "ChIJXSModoWLGGARILWiCfeu2M0",
                "JP",
                "Tokyo"
            ));
            locations.add(new LocationDto(
                "loc-004",
                "Shibuya Crossing",
                "Shibuya Crossing, Tokyo, Japan",
                "Shibuya City, Tokyo 150-0002, Japan",
                new BigDecimal("35.6598"),
                new BigDecimal("139.7006"),
                "ChIJXSModoWLGGARILWiCfeu2M0",
                "JP",
                "Tokyo"
            ));
        }
        
        if (lowerQuery.contains("new york") || lowerQuery.contains("nyc") || lowerQuery.contains("usa")) {
            locations.add(new LocationDto(
                "loc-005",
                "New York, NY, USA",
                "New York, NY, USA",
                "New York, NY, USA",
                new BigDecimal("40.7128"),
                new BigDecimal("-74.0060"),
                "ChIJOwg_06VPwokRYv534QaPC8g",
                "US",
                "New York"
            ));
            locations.add(new LocationDto(
                "loc-006",
                "Times Square",
                "Times Square, New York, NY, USA",
                "Times Square, New York, NY 10036, USA",
                new BigDecimal("40.7580"),
                new BigDecimal("-73.9855"),
                "ChIJmQJIxlVYwokRLgeuocVOGVU",
                "US",
                "New York"
            ));
            locations.add(new LocationDto(
                "loc-007",
                "Central Park",
                "Central Park, New York, NY, USA",
                "Central Park, New York, NY, USA",
                new BigDecimal("40.7829"),
                new BigDecimal("-73.9654"),
                "ChIJcbQ3lVZYwokRrQ5xeXlNxXk",
                "US",
                "New York"
            ));
        }
        
        if (lowerQuery.contains("london") || lowerQuery.contains("uk") || lowerQuery.contains("england")) {
            locations.add(new LocationDto(
                "loc-008",
                "London, UK",
                "London, UK",
                "London, UK",
                new BigDecimal("51.5074"),
                new BigDecimal("-0.1278"),
                "ChIJdd4hrwug2EcRmSrV3Vo6llI",
                "GB",
                "London"
            ));
            locations.add(new LocationDto(
                "loc-009",
                "Big Ben",
                "Big Ben, London, UK",
                "Westminster, London SW1A 0AA, UK",
                new BigDecimal("51.4994"),
                new BigDecimal("-0.1245"),
                "ChIJdd4hrwug2EcRmSrV3Vo6llI",
                "GB",
                "London"
            ));
        }
        
        if (lowerQuery.contains("bali") || lowerQuery.contains("indonesia")) {
            locations.add(new LocationDto(
                "loc-010",
                "Bali, Indonesia",
                "Bali, Indonesia",
                "Bali, Indonesia",
                new BigDecimal("-8.3405"),
                new BigDecimal("115.0920"),
                "ChIJnUvjRazaTC0R8l-HW3U3vZY",
                "ID",
                "Bali"
            ));
            locations.add(new LocationDto(
                "loc-011",
                "Ubud",
                "Ubud, Bali, Indonesia",
                "Ubud, Gianyar Regency, Bali, Indonesia",
                new BigDecimal("-8.5069"),
                new BigDecimal("115.2625"),
                "ChIJnUvjRazaTC0R8l-HW3U3vZY",
                "ID",
                "Bali"
            ));
        }
        
        if (lowerQuery.contains("dubai") || lowerQuery.contains("uae")) {
            locations.add(new LocationDto(
                "loc-012",
                "Dubai, UAE",
                "Dubai, United Arab Emirates",
                "Dubai, United Arab Emirates",
                new BigDecimal("25.2048"),
                new BigDecimal("55.2708"),
                "ChIJX8lVZUxJXz4R3L8W8pQxZqM",
                "AE",
                "Dubai"
            ));
            locations.add(new LocationDto(
                "loc-013",
                "Burj Khalifa",
                "Burj Khalifa, Dubai, UAE",
                "1 Sheikh Mohammed bin Rashid Blvd, Dubai, UAE",
                new BigDecimal("25.1972"),
                new BigDecimal("55.2744"),
                "ChIJX8lVZUxJXz4R3L8W8pQxZqM",
                "AE",
                "Dubai"
            ));
        }
        
        if (lowerQuery.contains("sydney") || lowerQuery.contains("australia")) {
            locations.add(new LocationDto(
                "loc-014",
                "Sydney, Australia",
                "Sydney NSW, Australia",
                "Sydney NSW, Australia",
                new BigDecimal("-33.8688"),
                new BigDecimal("151.2093"),
                "ChIJP3Sa8ziYEmsRUKgyFmh9AQM",
                "AU",
                "Sydney"
            ));
            locations.add(new LocationDto(
                "loc-015",
                "Sydney Opera House",
                "Sydney Opera House, Sydney, Australia",
                "Bennelong Point, Sydney NSW 2000, Australia",
                new BigDecimal("-33.8568"),
                new BigDecimal("151.2153"),
                "ChIJP3Sa8ziYEmsRUKgyFmh9AQM",
                "AU",
                "Sydney"
            ));
        }
        
        // Generic locations if no specific match
        if (locations.isEmpty()) {
            locations.add(new LocationDto(
                "loc-100",
                query + " (Search Result)",
                query,
                query,
                new BigDecimal("40.7128"),
                new BigDecimal("-74.0060"),
                "dummy-place-id-1",
                "US",
                "City"
            ));
            locations.add(new LocationDto(
                "loc-101",
                query + " Downtown",
                query + " Downtown",
                query + " Downtown",
                new BigDecimal("40.7580"),
                new BigDecimal("-73.9855"),
                "dummy-place-id-2",
                "US",
                "City"
            ));
            locations.add(new LocationDto(
                "loc-102",
                query + " Airport",
                query + " Airport",
                query + " International Airport",
                new BigDecimal("40.6413"),
                new BigDecimal("-73.7781"),
                "dummy-place-id-3",
                "US",
                "City"
            ));
        }
        
        return locations;
    }
    
    @Override
    public LocationDto createLocation(String name, String displayName, String address) {
        Location location = Location.builder()
            .name(name)
            .displayName(displayName)
            .address(address)
            .build();
        location = locationRepository.save(location);
        return toDto(location);
    }
    
    private LocationDto toDto(Location location) {
        return new LocationDto(
            location.getId(),
            location.getName(),
            location.getDisplayName(),
            location.getAddress(),
            location.getLatitude(),
            location.getLongitude(),
            location.getPlaceId(),
            location.getCountryCode(),
            location.getCity()
        );
    }
}

