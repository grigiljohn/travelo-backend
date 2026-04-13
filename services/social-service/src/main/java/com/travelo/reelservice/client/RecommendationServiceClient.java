package com.travelo.reelservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.travelo.commons.config.ResilientWebClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client to interact with recommendation service for ML-based reel recommendations.
 */
@Component
public class RecommendationServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationServiceClient.class);

    private final WebClient webClient;
    private final String recommendationServiceUrl;

    public RecommendationServiceClient(ResilientWebClientConfig resilientWebClientConfig,
                                      @Value("${app.recommendation-service.url:http://localhost:8091}") String recommendationServiceUrl) {
        this.recommendationServiceUrl = recommendationServiceUrl;
        this.webClient = resilientWebClientConfig.createResilientWebClient("recommendation-service", recommendationServiceUrl);
    }

    /**
     * Get recommended reel IDs for a user (ML inference).
     */
    public List<UUID> getRecommendedReels(String userId, int limit) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/recommendations/reels")
                            .queryParam("userId", userId)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToFlux(UUID.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            logger.error("Error fetching recommended reels for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Get recommendation score for a reel for a specific user.
     */
    public Double getReelRecommendationScore(UUID reelId, String userId) {
        try {
            return webClient.get()
                    .uri("/api/v1/recommendations/reels/{reelId}/score", reelId)
                    .header("X-User-Id", userId)
                    .retrieve()
                    .bodyToMono(Double.class)
                    .block();
        } catch (Exception e) {
            logger.error("Error fetching recommendation score for reel {}: {}", reelId, e.getMessage());
            return 0.0;
        }
    }

    /**
     * Request batch update of recommendation scores for reels.
     */
    public void requestBatchRecommendationUpdate(List<UUID> reelIds) {
        try {
            Map<String, Object> request = Map.of("reel_ids", reelIds);

            webClient.post()
                    .uri("/api/v1/recommendations/reels/batch-update")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .doOnError(e -> logger.error("Error requesting batch recommendation update: {}", e.getMessage()))
                    .subscribe();

        } catch (Exception e) {
            logger.error("Error requesting batch recommendation update: {}", e.getMessage());
        }
    }
}

