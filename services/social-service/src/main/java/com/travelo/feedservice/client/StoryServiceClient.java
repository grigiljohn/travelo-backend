package com.travelo.feedservice.client;

import com.travelo.commons.config.ResilientWebClientConfig;
import com.travelo.feedservice.client.dto.StoryPreviewDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

/**
 * Client for story-service discover/feed previews used by feed composition.
 */
@Component
public class StoryServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(StoryServiceClient.class);

    private final WebClient webClient;

    public StoryServiceClient(
            ResilientWebClientConfig resilientWebClientConfig,
            @Value("${app.story-service.url:http://localhost:8086}") String storyServiceBaseUrl) {
        this.webClient = resilientWebClientConfig.createResilientWebClient("story-service", storyServiceBaseUrl);
        logger.info("StoryServiceClient initialized with base URL: {}", storyServiceBaseUrl);
    }

    /**
     * Active stories for the stories strip (same contract as mobile {@code /discover}).
     */
    public List<StoryPreviewDto> fetchDiscoverStories(String viewerUserId) {
        logger.debug("Fetching discover stories for viewerUserId={}", viewerUserId);
        try {
            List<StoryPreviewDto> stories = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/stories/discover")
                            .queryParam("viewerUserId", viewerUserId)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<StoryPreviewDto>>() {})
                    .block();
            int n = stories != null ? stories.size() : 0;
            logger.debug("Retrieved {} discover stories", n);
            return stories != null ? stories : List.of();
        } catch (WebClientResponseException e) {
            logger.warn("story-service discover returned status={}", e.getStatusCode());
            return List.of();
        } catch (WebClientRequestException e) {
            logger.warn("Story-service unavailable, feed continues without story previews: {}", e.getMessage());
            return List.of();
        }
    }
}
