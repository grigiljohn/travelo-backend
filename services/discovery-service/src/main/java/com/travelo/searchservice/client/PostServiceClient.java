package com.travelo.searchservice.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelo.commons.config.ResilientWebClientConfig;
import com.travelo.searchservice.client.dto.ApiResponse;
import com.travelo.searchservice.client.dto.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

/**
 * Client to fetch post data from post-service for indexing and re-indexing.
 */
@Component
public class PostServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(PostServiceClient.class);

    private final WebClient webClient;
    private final String postServiceUrl;

    public PostServiceClient(ResilientWebClientConfig resilientWebClientConfig,
                            @Value("${app.post-service.url:http://localhost:8096}") String postServiceUrl) {
        this.postServiceUrl = postServiceUrl;
        this.webClient = resilientWebClientConfig.createResilientWebClient("post-service", postServiceUrl);
    }

    /**
     * Fetch all posts with pagination for re-indexing.
     */
    public List<PostDto> getAllPosts(int page, int limit) {
        try {
            logger.debug("Fetching posts - page: {}, limit: {}", page, limit);

            ApiResponse<PageResponse<PostDto>> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/posts")
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<PageResponse<PostDto>>>() {})
                    .block();

            if (logger.isDebugEnabled()) {
                logger.debug("PostServiceClient raw response: {}", response);
                if (response != null && response.getData() != null && response.getData().getData() != null) {
                    List<PostDto> posts = response.getData().getData();
                    logger.debug("PostServiceClient posts count: {}", posts.size());
                    posts.stream().limit(5).forEach(p ->
                            logger.debug("post id={} postType={} caption={}", p.getId(), p.getPostType(), p.getCaption()));
                }
            }

            if (response != null && response.getData() != null && response.getData().getData() != null) {
                return response.getData().getData();
            }
            return List.of();
        } catch (WebClientResponseException e) {
            logger.error("Error fetching posts from post-service: status={}", e.getStatusCode(), e);
            return List.of();
        } catch (Exception e) {
            logger.error("Error fetching posts: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Fetch post details for indexing (optional - can use event data).
     */
    public PostDto getPost(String postId) {
        try {
            logger.debug("Fetching post {} from post-service", postId);
            return null;
        } catch (Exception e) {
            logger.error("Error fetching post {}: {}", postId, e.getMessage());
            return null;
        }
    }

    /**
     * Post DTO for re-indexing.
     */
    public static class PostDto {
        private String id;
        private String caption;
        private String authorId;
        private String username;
        @JsonProperty("post_type")
        private String postType;
        private String mood;
        private String location;
        @JsonProperty("thumbnail_url")
        private String thumbnailUrl;
        @JsonProperty("media_items")
        private List<MediaItem> mediaItems;
        private Integer likes;
        private Integer comments;
        private Integer shares;
        private String createdAt;
        private String updatedAt;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getCaption() { return caption; }
        public void setCaption(String caption) { this.caption = caption; }
        public String getAuthorId() { return authorId; }
        public void setAuthorId(String authorId) { this.authorId = authorId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPostType() { return postType; }
        public void setPostType(String postType) { this.postType = postType; }
        public String getMood() { return mood; }
        public void setMood(String mood) { this.mood = mood; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getThumbnailUrl() { return thumbnailUrl; }
        public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
        public List<MediaItem> getMediaItems() { return mediaItems; }
        public void setMediaItems(List<MediaItem> mediaItems) { this.mediaItems = mediaItems; }
        public Integer getLikes() { return likes; }
        public void setLikes(Integer likes) { this.likes = likes; }
        public Integer getComments() { return comments; }
        public void setComments(Integer comments) { this.comments = comments; }
        public Integer getShares() { return shares; }
        public void setShares(Integer shares) { this.shares = shares; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

        public static class MediaItem {
            @JsonProperty("url")
            private String downloadUrl;
            @JsonProperty("download_url")
            private String downloadUrlAlt;
            @JsonProperty("thumbnail_url")
            private String thumbnailUrl;

            public String getDownloadUrl() { return downloadUrl; }
            public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
            public String getDownloadUrlAlt() { return downloadUrlAlt; }
            public void setDownloadUrlAlt(String downloadUrlAlt) { this.downloadUrlAlt = downloadUrlAlt; }
            public String getThumbnailUrl() { return thumbnailUrl; }
            public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
        }
    }
}

