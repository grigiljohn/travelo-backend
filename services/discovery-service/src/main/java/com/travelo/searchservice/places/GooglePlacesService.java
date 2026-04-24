package com.travelo.searchservice.places;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Server-side Google Places (legacy REST) — keeps API keys off mobile clients.
 */
@Service
public class GooglePlacesService {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(12);
    private static final int MAX_AUTOCOMPLETE = 15;
    private static final int MAX_DETAIL_FETCH = 10;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public GooglePlacesService(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.google.places.api-key:}") String apiKey) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.webClient = webClientBuilder.baseUrl("https://maps.googleapis.com/maps/api/place").build();
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    /**
     * Autocomplete + Place Details (coordinates) for discovery / tagging UIs.
     */
    public List<ResolvedPlace> autocompleteWithDetails(String input, int limit) {
        if (!isConfigured() || input == null || input.isBlank()) {
            return List.of();
        }
        int cap = Math.min(Math.max(limit, 1), MAX_AUTOCOMPLETE);
        List<JsonNode> predictions = fetchAutocompletePredictions(input.trim(), cap);
        List<ResolvedPlace> out = new ArrayList<>();
        int detailBudget = Math.min(predictions.size(), Math.min(cap, MAX_DETAIL_FETCH));
        for (int i = 0; i < detailBudget; i++) {
            JsonNode p = predictions.get(i);
            String placeId = text(p, "place_id");
            if (placeId.isEmpty()) {
                continue;
            }
            Optional<ResolvedPlace> resolved = fetchPlaceDetails(placeId);
            if (resolved.isPresent()) {
                out.add(resolved.get());
            } else {
                String title = predictionTitle(p);
                String subtitle = predictionSubtitle(p);
                out.add(new ResolvedPlace(placeId, title, subtitle, 0, 0, ""));
            }
        }
        return out;
    }

    public List<ResolvedPlace> nearbyPlaces(double lat, double lng, int limit) {
        if (!isConfigured()) {
            return List.of();
        }
        int cap = Math.min(Math.max(limit, 1), 20);
        try {
            URI uri = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host("maps.googleapis.com")
                    .path("/maps/api/place/nearbysearch/json")
                    .queryParam("location", lat + "," + lng)
                    .queryParam("radius", 5000)
                    .queryParam("key", apiKey)
                    .build(true)
                    .toUri();
            String body = webClient.get().uri(uri).retrieve().bodyToMono(String.class).block(TIMEOUT);
            JsonNode root = objectMapper.readTree(body);
            if (!"OK".equals(text(root, "status")) && !"ZERO_RESULTS".equals(text(root, "status"))) {
                log.warn("Google nearbysearch status={} error_message={}", text(root, "status"), text(root, "error_message"));
                return List.of();
            }
            JsonNode results = root.get("results");
            if (results == null || !results.isArray()) {
                return List.of();
            }
            List<ResolvedPlace> out = new ArrayList<>();
            for (JsonNode r : results) {
                if (out.size() >= cap) {
                    break;
                }
                String placeId = text(r, "place_id");
                String name = text(r, "name");
                String vicinity = text(r, "vicinity");
                JsonNode loc = r.path("geometry").path("location");
                double plat = loc.path("lat").asDouble(0);
                double plng = loc.path("lng").asDouble(0);
                if (placeId.isEmpty() || name.isEmpty()) {
                    continue;
                }
                out.add(new ResolvedPlace(placeId, name, vicinity, plat, plng, vicinity));
            }
            return out;
        } catch (Exception e) {
            log.warn("Google nearby failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Place Details — used by plan flow for extra fields (e.g. photo reference).
     */
    public Optional<Map<String, Object>> fetchPlaceDetailsMap(String placeId) {
        return fetchPlaceDetails(placeId).map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("place_id", r.placeId());
            m.put("name", r.primaryName());
            m.put("formatted_address", r.formattedAddress());
            m.put("lat", r.lat());
            m.put("lng", r.lng());
            if (r.photoReference() != null && !r.photoReference().isEmpty()) {
                m.put("photo_reference", r.photoReference());
            }
            return m;
        });
    }

    private Optional<ResolvedPlace> fetchPlaceDetails(String placeId) {
        try {
            URI uri = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host("maps.googleapis.com")
                    .path("/maps/api/place/details/json")
                    .queryParam("place_id", placeId)
                    .queryParam(
                            "fields",
                            "place_id,name,formatted_address,geometry/location,photos,vicinity")
                    .queryParam("key", apiKey)
                    .build(true)
                    .toUri();
            String body = webClient.get().uri(uri).retrieve().bodyToMono(String.class).block(TIMEOUT);
            JsonNode root = objectMapper.readTree(body);
            if (!"OK".equals(text(root, "status"))) {
                log.debug("Place details status={} for place_id={}", text(root, "status"), placeId);
                return Optional.empty();
            }
            JsonNode result = root.get("result");
            if (result == null || result.isNull()) {
                return Optional.empty();
            }
            String name = text(result, "name");
            String formatted = text(result, "formatted_address");
            String vicinity = text(result, "vicinity");
            JsonNode loc = result.path("geometry").path("location");
            double lat = loc.path("lat").asDouble(0);
            double lng = loc.path("lng").asDouble(0);
            String photoRef = "";
            JsonNode photos = result.get("photos");
            if (photos != null && photos.isArray() && photos.size() > 0) {
                photoRef = text(photos.get(0), "photo_reference");
            }
            String subtitle = !vicinity.isEmpty() ? vicinity : formatted;
            return Optional.of(
                    new ResolvedPlace(placeId, name, subtitle, lat, lng, formatted, photoRef));
        } catch (Exception e) {
            log.debug("Place details failed for {}: {}", placeId, e.getMessage());
            return Optional.empty();
        }
    }

    private List<JsonNode> fetchAutocompletePredictions(String input, int limit) {
        try {
            URI uri = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host("maps.googleapis.com")
                    .path("/maps/api/place/autocomplete/json")
                    .queryParam("input", input)
                    .queryParam("key", apiKey)
                    .build(true)
                    .toUri();
            String body = webClient.get().uri(uri).retrieve().bodyToMono(String.class).block(TIMEOUT);
            JsonNode root = objectMapper.readTree(body);
            String status = text(root, "status");
            if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                log.warn("Google autocomplete status={} error_message={}", status, text(root, "error_message"));
                return List.of();
            }
            JsonNode preds = root.get("predictions");
            if (preds == null || !preds.isArray()) {
                return List.of();
            }
            List<JsonNode> list = new ArrayList<>();
            for (JsonNode p : preds) {
                if (list.size() >= limit) {
                    break;
                }
                list.add(p);
            }
            return list;
        } catch (Exception e) {
            log.warn("Google autocomplete failed: {}", e.getMessage());
            return List.of();
        }
    }

    private static String predictionTitle(JsonNode p) {
        JsonNode sf = p.get("structured_formatting");
        if (sf != null && sf.hasNonNull("main_text")) {
            return sf.get("main_text").asText("");
        }
        return text(p, "description");
    }

    private static String predictionSubtitle(JsonNode p) {
        JsonNode sf = p.get("structured_formatting");
        if (sf != null && sf.hasNonNull("secondary_text")) {
            return sf.get("secondary_text").asText("");
        }
        return "";
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isNull()) {
            return "";
        }
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? "" : v.asText("");
    }

    public Map<String, Object> toApiLocationRow(ResolvedPlace r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.placeId());
        m.put("name", r.primaryName());
        m.put("subtitle", r.secondaryText());
        m.put("type", "place");
        m.put("lat", r.lat());
        m.put("lng", r.lng());
        return m;
    }

    public record ResolvedPlace(
            String placeId,
            String primaryName,
            String secondaryText,
            double lat,
            double lng,
            String formattedAddress,
            String photoReference) {

        public ResolvedPlace(
                String placeId,
                String primaryName,
                String secondaryText,
                double lat,
                double lng,
                String formattedAddress) {
            this(placeId, primaryName, secondaryText, lat, lng, formattedAddress, "");
        }
    }
}
