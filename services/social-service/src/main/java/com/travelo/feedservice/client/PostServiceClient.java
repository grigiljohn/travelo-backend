package com.travelo.feedservice.client;

import com.travelo.feedservice.client.dto.PostDto;
import com.travelo.feedservice.client.dto.PageResponse;
import com.travelo.feedservice.client.dto.ApiResponse;
import com.travelo.feedservice.service.FeedMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.travelo.commons.config.ResilientWebClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Client for interacting with post-service.
 */
@Component("feedPostServiceClient")
public class PostServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(PostServiceClient.class);

    private final WebClient webClient;
    private final boolean mockFallbackEnabled;
    private final String mockFallbackMode;
    private final FeedMetricsService feedMetricsService;

    public PostServiceClient(
            ResilientWebClientConfig resilientWebClientConfig,
            @Value("${app.post-service.url:http://localhost:8096}") String postServiceBaseUrl,
            @Value("${app.feed.mock-fallback-enabled:true}") boolean mockFallbackEnabled,
            @Value("${app.feed.mock-fallback-mode:on_error}") String mockFallbackMode,
            FeedMetricsService feedMetricsService) {
        this.webClient = resilientWebClientConfig.createResilientWebClient("post-service", postServiceBaseUrl);
        this.mockFallbackEnabled = mockFallbackEnabled;
        this.mockFallbackMode = mockFallbackMode == null ? "on_error" : mockFallbackMode.trim().toLowerCase();
        this.feedMetricsService = feedMetricsService;
        logger.info("PostServiceClient initialized with base URL: {}", postServiceBaseUrl);
    }

    /**
     * Fetch posts from post-service.
     */
    public List<PostDto> getPosts(
            int page, int limit, String mood, List<String> authorUserIds, String viewerUserId) {
        logger.debug("Fetching posts - page: {}, limit: {}, mood: {}, authorCount={}, viewer={}",
                page, limit, mood, authorUserIds == null ? 0 : authorUserIds.size(), viewerUserId);
        if (shouldServeAlwaysMock()) {
            feedMetricsService.recordFallbackUsed("post-service", "always");
            return buildMockPosts(limit);
        }

        try {
            List<String> authors = authorUserIds == null ? List.of() : authorUserIds.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .toList();
            var uriBuilder = webClient.get()
                    .uri(uriBuilder1 -> {
                        var builder = uriBuilder1.path("/api/v1/posts")
                                .queryParam("page", page)
                                .queryParam("limit", limit);
                        if (mood != null && !mood.isEmpty()) {
                            builder = builder.queryParam("mood", mood);
                        }
                        for (String aid : authors) {
                            builder = builder.queryParam("author_ids", aid);
                        }
                        if (viewerUserId != null && !viewerUserId.isBlank()) {
                            builder = builder.queryParam("viewer_id", viewerUserId);
                        }
                        return builder.build();
                    });

            ApiResponse<PageResponse<PostDto>> apiResponse = uriBuilder
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<ApiResponse<PageResponse<PostDto>>>() {})
                    .block();

            if (apiResponse == null || apiResponse.getData() == null) {
                logger.warn("No posts data in response");
                return List.of();
            }

            PageResponse<PostDto> pageResponse = apiResponse.getData();
            logger.debug("Retrieved {} posts", pageResponse.getData() != null ? pageResponse.getData().size() : 0);
            return pageResponse.getData() != null ? pageResponse.getData() : List.of();
        } catch (WebClientResponseException e) {
            logger.error("Error fetching posts from post-service: status={}", e.getStatusCode(), e);
            if (shouldFallbackOnError()) {
                feedMetricsService.recordFallbackUsed("post-service", "on_error");
                return buildMockPosts(limit);
            }
            throw new RuntimeException("Failed to fetch posts from post-service: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error fetching posts from post-service", e);
            if (shouldFallbackOnError()) {
                feedMetricsService.recordFallbackUsed("post-service", "on_error");
                return buildMockPosts(limit);
            }
            throw e;
        }
    }

    public List<PostDto> getPosts(int page, int limit, String mood, List<String> authorUserIds) {
        return getPosts(page, limit, mood, authorUserIds, null);
    }

    private boolean shouldServeAlwaysMock() {
        return mockFallbackEnabled && "always".equals(mockFallbackMode);
    }

    private boolean shouldFallbackOnError() {
        if (!mockFallbackEnabled) {
            return false;
        }
        return !"off".equals(mockFallbackMode);
    }

    private List<PostDto> buildMockPosts(int limit) {
        int safeLimit = Math.max(6, Math.min(limit, 30));
        List<PostDto> posts = new ArrayList<>(safeLimit);
        for (int i = 0; i < safeLimit; i++) {
            boolean reel = i % 4 == 2;
            PostDto post = new PostDto();
            post.setId(UUID.nameUUIDFromBytes(("mock-post-" + i).getBytes()).toString());
            post.setUserId(UUID.nameUUIDFromBytes(("mock-user-" + (i % 10)).getBytes()).toString());
            post.setUsername("traveler_" + (120 + i % 10));
            post.setUserAvatar("https://picsum.photos/seed/travelo-avatar-" + i + "/120/120");
            post.setPostType(reel ? "reel" : "post");
            post.setCaption(reel
                    ? "Quick reel from hidden gems in Bali. #travel #reel"
                    : "Sunrise trail with mountain coffee stops. #travelo");
            post.setContent(post.getCaption());
            post.setLocation(i % 2 == 0 ? "Ubud, Bali" : "Kasol, India");
            post.setLikes(80 + (i * 13));
            post.setComments(6 + (i * 3));
            post.setIsLiked(false);
            post.setIsSaved(false);
            post.setCreatedAt(Instant.now().minusSeconds(3600L * (i + 1)).toString());
            if (reel) {
                post.setThumbnailUrl("https://picsum.photos/seed/travelo-reel-thumb-" + i + "/720/1280");
                PostDto.MediaItemDto mediaItem = new PostDto.MediaItemDto();
                mediaItem.setId(UUID.nameUUIDFromBytes(("mock-media-" + i).getBytes()).toString());
                mediaItem.setType("video");
                mediaItem.setUrl("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4");
                mediaItem.setThumbnailUrl(post.getThumbnailUrl());
                post.setMediaItems(List.of(mediaItem));
                post.setImages(List.of(post.getThumbnailUrl()));
            } else {
                String image = "https://picsum.photos/seed/travelo-feed-image-" + i + "/1080/1350";
                post.setImages(List.of(image));
                post.setThumbnailUrl(image);
            }
            posts.add(post);
        }
        logger.warn("Serving {} mock posts from feed fallback", posts.size());
        return posts;
    }
}

