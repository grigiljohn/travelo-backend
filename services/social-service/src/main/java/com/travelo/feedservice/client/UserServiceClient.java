package com.travelo.feedservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Client to interact with user-service to fetch following relationships.
 */
@Component("feedUserServiceClient")
public class UserServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);

    private final WebClient webClient;
    private final String userServiceUrl;

    public UserServiceClient(WebClient.Builder webClientBuilder, 
                            @Value("${app.user-service.url:http://localhost:8081}") String userServiceUrl) {
        this.userServiceUrl = userServiceUrl;
        this.webClient = webClientBuilder.baseUrl(userServiceUrl).build();
    }

    /**
     * Get list of user IDs that the given user follows.
     * @param userId User ID
     * @return List of followed user IDs
     */
    public List<UUID> getFollowing(UUID userId) {
        try {
            // TODO: Implement actual API call once user-service exposes following endpoint
            // For now, return empty list as placeholder
            logger.debug("Fetching following list for user {} from user-service", userId);
            
            // Example API call (uncomment when user-service implements this):
            /*
            return webClient.get()
                    .uri("/api/v1/users/{userId}/following", userId)
                    .retrieve()
                    .bodyToFlux(UUID.class)
                    .collectList()
                    .block();
            */
            
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Error fetching following list for user {}: {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Check if a user follows another user.
     */
    public boolean isFollowing(UUID userId, UUID targetUserId) {
        List<UUID> following = getFollowing(userId);
        return following.contains(targetUserId);
    }

    /**
     * Get the list of users that follow the given author.
     * Used by the feed fan-out pipeline on `post.created` events to invalidate /
     * recompute follower feeds.
     *
     * <p>Fail-open: if identity-service hasn't implemented the endpoint yet (or
     * the call fails), we return an empty list so the caller can gracefully fall
     * back to lazy recomputation on next feed read.
     */
    public List<UUID> getFollowers(UUID authorId) {
        try {
            logger.debug("Fetching followers for user {} from user-service", authorId);
            List<UUID> followers = webClient.get()
                    .uri("/api/v1/users/{authorId}/followers", authorId)
                    .retrieve()
                    .bodyToFlux(UUID.class)
                    .collectList()
                    .blockOptional()
                    .orElseGet(ArrayList::new);
            logger.debug("Fetched {} followers for user {}", followers.size(), authorId);
            return followers;
        } catch (Exception e) {
            // Expected while identity-service is still wiring up the followers endpoint —
            // downgrade to debug after roll-out.
            logger.warn("Error fetching followers for user {}: {}", authorId, e.getMessage());
            return new ArrayList<>();
        }
    }
}

