package com.travelo.reelservice.client;

import com.travelo.reelservice.client.dto.PostDto;
import com.travelo.reelservice.client.dto.PageResponse;
import com.travelo.reelservice.client.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.travelo.commons.config.ResilientWebClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Client for interacting with post-service to fetch reels.
 */
@Component("reelPostServiceClient")
public class PostServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(PostServiceClient.class);

    private final WebClient webClient;

    public PostServiceClient(
            ResilientWebClientConfig resilientWebClientConfig,
            @Value("${app.post-service.url:http://localhost:8096}") String postServiceBaseUrl) {
        this.webClient = resilientWebClientConfig.createResilientWebClient("post-service", postServiceBaseUrl);
        logger.info("PostServiceClient initialized with base URL: {}", postServiceBaseUrl);
    }

    /**
     * Fetch reels (posts with post_type = REEL) from post-service.
     */
    public List<PostDto> getReels(int page, int limit, String mood) {
        logger.debug("Fetching reels - page: {}, limit: {}, mood: {}", page, limit, mood);

        try {
            var uriBuilder = webClient.get()
                    .uri(uriBuilder1 -> {
                        var builder = uriBuilder1.path("/api/v1/posts")
                                .queryParam("page", page)
                                .queryParam("limit", limit * 2); // Fetch more to filter for REEL type
                        if (mood != null && !mood.isEmpty()) {
                            builder.queryParam("mood", mood);
                        }
                        return builder.build();
                    });

            ApiResponse<PageResponse<PostDto>> apiResponse = uriBuilder
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<ApiResponse<PageResponse<PostDto>>>() {})
                    .block();

            if (apiResponse == null || apiResponse.getData() == null) {
                logger.warn("No reels data in response");
                return List.of();
            }

            PageResponse<PostDto> pageResponse = apiResponse.getData();
            List<PostDto> allPosts = pageResponse.getData() != null ? pageResponse.getData() : List.of();
            
            // Filter for REEL type posts
            List<PostDto> reels = allPosts.stream()
                    .filter(post -> "reel".equalsIgnoreCase(post.getPostType()))
                    .limit(limit)
                    .collect(Collectors.toList());

            logger.debug("Retrieved {} reels (filtered from {} posts)", reels.size(), allPosts.size());
            return reels;
        } catch (WebClientResponseException e) {
            logger.error("Error fetching reels from post-service: status={}, message={}", 
                    e.getStatusCode(), e.getMessage(), e);
            throw new RuntimeException("Failed to fetch reels from post-service: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error fetching reels from post-service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch reels from post-service: " + e.getMessage(), e);
        }
    }
}

