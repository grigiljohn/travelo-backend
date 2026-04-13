package com.travelo.feedservice.client;

import com.travelo.feedservice.client.dto.AdDeliveryRequest;
import com.travelo.feedservice.client.dto.AdDeliveryResponse;
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

    public AdServiceClient(
            ResilientWebClientConfig resilientWebClientConfig,
            @Value("${app.ad-service.url:http://localhost:8093}") String adServiceBaseUrl) {
        this.webClient = resilientWebClientConfig.createResilientWebClient("ad-service", adServiceBaseUrl);
        logger.info("AdServiceClient initialized with base URL: {}", adServiceBaseUrl);
    }

    /**
     * Fetch ads for a specific placement.
     */
    public List<AdDeliveryResponse> fetchAds(String placement, UUID userId, Map<String, Object> userContext, int count) {
        logger.debug("Fetching {} ads for placement: {}, userId: {}", count, placement, userId);

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
            // Don't fail feed if ads fail - return empty list
            return List.of();
        } catch (WebClientRequestException e) {
            logger.warn("Ad-service unavailable, continuing feed without ads: {}", e.getMessage());
            return List.of();
        }
    }
}

