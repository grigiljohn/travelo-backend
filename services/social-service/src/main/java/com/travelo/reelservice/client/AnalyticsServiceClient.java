package com.travelo.reelservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.travelo.commons.config.ResilientWebClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;

/**
 * Client to send analytics events for reels.
 */
@Component
public class AnalyticsServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsServiceClient.class);

    private final WebClient webClient;
    private final String analyticsServiceUrl;

    public AnalyticsServiceClient(ResilientWebClientConfig resilientWebClientConfig,
                                 @Value("${app.analytics-service.url:http://localhost:8099}") String analyticsServiceUrl) {
        this.analyticsServiceUrl = analyticsServiceUrl;
        this.webClient = resilientWebClientConfig.createResilientWebClient("analytics-service", analyticsServiceUrl);
    }

    /**
     * Track reel view event.
     */
    public void trackReelView(UUID reelId, String userId, Integer viewDuration, Double completionPercentage) {
        try {
            Map<String, Object> event = Map.of(
                    "event_type", "reel_view",
                    "reel_id", reelId.toString(),
                    "user_id", userId,
                    "view_duration_seconds", viewDuration != null ? viewDuration : 0,
                    "completion_percentage", completionPercentage != null ? completionPercentage : 0.0
            );

            webClient.post()
                    .uri("/api/v1/events")
                    .bodyValue(event)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .doOnError(e -> logger.error("Error tracking reel view: {}", e.getMessage()))
                    .subscribe();

        } catch (Exception e) {
            logger.error("Error tracking reel view event: {}", e.getMessage());
        }
    }

    /**
     * Track reel like event.
     */
    public void trackReelLike(UUID reelId, String userId) {
        try {
            Map<String, Object> event = Map.of(
                    "event_type", "reel_like",
                    "reel_id", reelId.toString(),
                    "user_id", userId
            );

            webClient.post()
                    .uri("/api/v1/events")
                    .bodyValue(event)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .doOnError(e -> logger.error("Error tracking reel like: {}", e.getMessage()))
                    .subscribe();

        } catch (Exception e) {
            logger.error("Error tracking reel like event: {}", e.getMessage());
        }
    }

    /**
     * Track reel comment event.
     */
    public void trackReelComment(UUID reelId, String userId) {
        try {
            Map<String, Object> event = Map.of(
                    "event_type", "reel_comment",
                    "reel_id", reelId.toString(),
                    "user_id", userId
            );

            webClient.post()
                    .uri("/api/v1/events")
                    .bodyValue(event)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .doOnError(e -> logger.error("Error tracking reel comment: {}", e.getMessage()))
                    .subscribe();

        } catch (Exception e) {
            logger.error("Error tracking reel comment event: {}", e.getMessage());
        }
    }
}

