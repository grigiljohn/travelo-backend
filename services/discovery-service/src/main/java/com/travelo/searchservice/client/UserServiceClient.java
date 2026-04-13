package com.travelo.searchservice.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.travelo.commons.config.ResilientWebClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Client to fetch user data from user-service for indexing and follow state enrichment.
 */
@Component
public class UserServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);

    private final WebClient webClient;
    private final String userServiceUrl;

    public UserServiceClient(ResilientWebClientConfig resilientWebClientConfig,
                            @Value("${app.user-service.url:http://localhost:8081}") String userServiceUrl) {
        this.userServiceUrl = userServiceUrl;
        this.webClient = resilientWebClientConfig.createResilientWebClient("user-service", userServiceUrl);
    }

    /**
     * Fetch user details for indexing (optional - can use event data).
     */
    public UserDto getUser(String userId) {
        try {
            // TODO: Implement API call to user-service if needed
            // For now, we rely on event data
            logger.debug("Fetching user {} from user-service", userId);
            return null;
        } catch (Exception e) {
            logger.error("Error fetching user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Search users in user-service and return with follow state
     * @param query Search query
     * @param viewerId Viewer's user ID (for follow state)
     * @param page Page number
     * @param limit Results per page
     * @return List of UserDto with follow state
     */
    public List<UserDto> searchUsersWithFollowState(String query, String viewerId, int page, int limit) {
        try {
            logger.info("Calling user-service to search users: query={}, viewerId={}, page={}, limit={}", query, viewerId, page, limit);
            
            Mono<List<UserDto>> responseMono = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/users/search")
                            .queryParam("q", query)
                            .queryParam("page", page)
                            .queryParam("limit", limit)
                            .queryParamIfPresent("viewer_id", viewerId != null && !viewerId.isEmpty() ? java.util.Optional.of(viewerId) : java.util.Optional.empty())
                            .build())
                    .retrieve()
                    .bodyToFlux(UserDto.class)
                    .collectList();
            
            List<UserDto> users = responseMono.block();
            
            if (users != null) {
                logger.info("Received {} users from user-service", users.size());
                return users;
            }
            
            logger.warn("Received null response from user-service");
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Failed to fetch users from user-service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch users from user-service: " + e.getMessage(), e);
        }
    }

    // UserDto for user-service response
    public static class UserDto {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("username")
        private String username;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("profile_picture_url")
        private String profilePictureUrl;
        
        @JsonProperty("is_verified")
        private Boolean isVerified;
        
        @JsonProperty("is_following")
        private Boolean isFollowing;
        
        @JsonProperty("followers_count")
        private Long followersCount;
        
        @JsonProperty("following_count")
        private Long followingCount;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getProfilePictureUrl() { return profilePictureUrl; }
        public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }
        public Boolean getIsVerified() { return isVerified; }
        public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
        public Boolean getIsFollowing() { return isFollowing; }
        public void setIsFollowing(Boolean isFollowing) { this.isFollowing = isFollowing; }
        public Long getFollowersCount() { return followersCount; }
        public void setFollowersCount(Long followersCount) { this.followersCount = followersCount; }
        public Long getFollowingCount() { return followingCount; }
        public void setFollowingCount(Long followingCount) { this.followingCount = followingCount; }
    }
}
