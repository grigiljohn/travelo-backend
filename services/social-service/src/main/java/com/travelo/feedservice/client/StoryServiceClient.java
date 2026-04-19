package com.travelo.feedservice.client;

import com.travelo.commons.config.ResilientWebClientConfig;
import com.travelo.feedservice.client.dto.StoryPreviewDto;
import com.travelo.feedservice.service.FeedMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.UUID;

/**
 * Client for story-service discover/feed previews used by feed composition.
 */
@Component
public class StoryServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(StoryServiceClient.class);

    private final WebClient webClient;
    private final boolean mockFallbackEnabled;
    private final String mockFallbackMode;
    private final FeedMetricsService feedMetricsService;

    public StoryServiceClient(
            ResilientWebClientConfig resilientWebClientConfig,
            @Value("${app.story-service.url:http://localhost:8086}") String storyServiceBaseUrl,
            @Value("${app.feed.mock-fallback-enabled:true}") boolean mockFallbackEnabled,
            @Value("${app.feed.mock-fallback-mode:on_error}") String mockFallbackMode,
            FeedMetricsService feedMetricsService) {
        this.webClient = resilientWebClientConfig.createResilientWebClient("story-service", storyServiceBaseUrl);
        this.mockFallbackEnabled = mockFallbackEnabled;
        this.mockFallbackMode = mockFallbackMode == null ? "on_error" : mockFallbackMode.trim().toLowerCase();
        this.feedMetricsService = feedMetricsService;
        logger.info("StoryServiceClient initialized with base URL: {}", storyServiceBaseUrl);
    }

    /**
     * Active stories for the stories strip (same contract as mobile {@code /discover}).
     */
    public List<StoryPreviewDto> fetchDiscoverStories(String viewerUserId) {
        logger.debug("Fetching discover stories for viewerUserId={}", viewerUserId);
        if (mockFallbackEnabled && "always".equals(mockFallbackMode)) {
            feedMetricsService.recordFallbackUsed("story-service", "always");
            return buildMockStories();
        }
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
            if (shouldFallbackOnError()) {
                feedMetricsService.recordFallbackUsed("story-service", "on_error");
                return buildMockStories();
            }
            return List.of();
        } catch (WebClientRequestException e) {
            logger.warn("Story-service unavailable, feed continues without story previews: {}", e.getMessage());
            if (shouldFallbackOnError()) {
                feedMetricsService.recordFallbackUsed("story-service", "on_error");
                return buildMockStories();
            }
            return List.of();
        }
    }

    private List<StoryPreviewDto> buildMockStories() {
        return java.util.stream.IntStream.range(0, 8)
                .mapToObj(i -> {
                    StoryPreviewDto dto = new StoryPreviewDto();
                    dto.setId(UUID.nameUUIDFromBytes(("mock-story-" + i).getBytes()));
                    dto.setUserId(UUID.nameUUIDFromBytes(("mock-story-user-" + i).getBytes()).toString());
                    dto.setUserName("story_traveler_" + (30 + i));
                    dto.setUserAvatar("https://picsum.photos/seed/travelo-story-avatar-" + i + "/120/120");
                    dto.setImageUrl("https://picsum.photos/seed/travelo-story-" + i + "/720/1280");
                    dto.setCaption("Story highlight " + (i + 1));
                    dto.setLocationLabel(i % 2 == 0 ? "Goa" : "Istanbul");
                    dto.setStoryType("image");
                    dto.setIsViewed(false);
                    return dto;
                })
                .toList();
    }

    private boolean shouldFallbackOnError() {
        if (!mockFallbackEnabled) {
            return false;
        }
        return !"off".equals(mockFallbackMode);
    }
}
