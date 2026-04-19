package com.travelo.feedservice.client;

import com.travelo.feedservice.client.dto.AdDeliveryRequest;
import com.travelo.feedservice.client.dto.AdDeliveryResponse;
import com.travelo.feedservice.service.FeedMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.travelo.commons.config.ResilientWebClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client for interacting with ad-service.
 */
@Component("feedAdServiceClient")
public class AdServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(AdServiceClient.class);

    private final WebClient webClient;
    private final boolean mockFallbackEnabled;
    private final String mockFallbackMode;
    private final FeedMetricsService feedMetricsService;

    public AdServiceClient(
            ResilientWebClientConfig resilientWebClientConfig,
            @Value("${app.ad-service.url:http://localhost:8093}") String adServiceBaseUrl,
            @Value("${app.feed.mock-fallback-enabled:true}") boolean mockFallbackEnabled,
            @Value("${app.feed.mock-fallback-mode:on_error}") String mockFallbackMode,
            FeedMetricsService feedMetricsService) {
        this.webClient = resilientWebClientConfig.createResilientWebClient("ad-service", adServiceBaseUrl);
        this.mockFallbackEnabled = mockFallbackEnabled;
        this.mockFallbackMode = mockFallbackMode == null ? "on_error" : mockFallbackMode.trim().toLowerCase();
        this.feedMetricsService = feedMetricsService;
        logger.info("AdServiceClient initialized with base URL: {}", adServiceBaseUrl);
    }

    /**
     * Fetch ads for a specific placement.
     */
    public List<AdDeliveryResponse> fetchAds(String placement, UUID userId, Map<String, Object> userContext, int count) {
        logger.debug("Fetching {} ads for placement: {}, userId: {}", count, placement, userId);
        if (mockFallbackEnabled && "always".equals(mockFallbackMode)) {
            feedMetricsService.recordFallbackUsed("ad-service", "always");
            return buildMockAds(count);
        }

        AdDeliveryRequest request = new AdDeliveryRequest(placement, userId, userContext);

        try {
            List<AdDeliveryResponse> ads = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/ads/delivery/fetch")
                            .queryParam("count", count)
                            .build())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<List<AdDeliveryResponse>>() {})
                    .block();

            logger.debug("Retrieved {} ads", ads != null ? ads.size() : 0);
            return ads != null ? ads : List.of();
        } catch (WebClientResponseException e) {
            logger.error("Error fetching ads from ad-service: status={}", e.getStatusCode(), e);
            if (shouldFallbackOnError()) {
                feedMetricsService.recordFallbackUsed("ad-service", "on_error");
                return buildMockAds(count);
            }
            return List.of();
        } catch (WebClientRequestException e) {
            logger.warn("Ad-service unavailable, continuing feed without ads: {}", e.getMessage());
            if (shouldFallbackOnError()) {
                feedMetricsService.recordFallbackUsed("ad-service", "on_error");
                return buildMockAds(count);
            }
            return List.of();
        }
    }

    private List<AdDeliveryResponse> buildMockAds(int count) {
        int safeCount = Math.max(1, Math.min(count, 5));
        return java.util.stream.IntStream.range(0, safeCount)
                .mapToObj(i -> new AdDeliveryResponse(
                        UUID.nameUUIDFromBytes(("mock-ad-" + i).getBytes()),
                        UUID.nameUUIDFromBytes(("mock-adgroup-" + i).getBytes()),
                        UUID.nameUUIDFromBytes(("mock-campaign-" + i).getBytes()),
                        "DISPLAY",
                        "feed",
                        Map.of(
                                "imageUrl", "https://picsum.photos/seed/travelo-ad-image-" + i + "/1080/1080",
                                "caption", "Upgrade your trip planning with premium trails."
                        ),
                        List.of("Sponsored escape deals " + (i + 1)),
                        List.of("Curated stay + local experiences for weekend travelers."),
                        "SHOP_NOW",
                        "Book now",
                        "https://travelo.example/offers/" + i,
                        "offers.travelo.example",
                        "Travelo Partners",
                        "https://travelo.example",
                        Map.of("fallback", true)
                ))
                .toList();
    }

    private boolean shouldFallbackOnError() {
        if (!mockFallbackEnabled) {
            return false;
        }
        return !"off".equals(mockFallbackMode);
    }
}

