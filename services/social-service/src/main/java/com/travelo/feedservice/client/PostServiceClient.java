package com.travelo.feedservice.client;

import com.travelo.feedservice.client.dto.PostDto;
import com.travelo.feedservice.client.dto.PageResponse;
import com.travelo.feedservice.client.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.travelo.commons.config.ResilientWebClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

/**
 * Client for interacting with post-service.
 */
@Component("feedPostServiceClient")
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
     * Fetch posts from post-service.
     */
    public List<PostDto> getPosts(int page, int limit, String mood, List<String> authorUserIds) {
        logger.debug("Fetching posts - page: {}, limit: {}, mood: {}, authorCount={}",
                page, limit, mood, authorUserIds == null ? 0 : authorUserIds.size());

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
            throw new RuntimeException("Failed to fetch posts from post-service: " + e.getMessage(), e);
        }
    }
}

