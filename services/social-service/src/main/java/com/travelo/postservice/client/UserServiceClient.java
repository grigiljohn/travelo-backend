package com.travelo.postservice.client;

import com.travelo.postservice.client.dto.UserDto;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.UUID;

/**
 * Client for interacting with user-service.
 * Handles all communication with the user microservice.
 */
@Component("postUserServiceClient")
public class UserServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);

    private WebClient webClient;

    @Autowired(required = false)
    private com.travelo.commons.config.ResilientWebClientConfig resilientWebClientConfig;

    @Value("${app.user-service.url:http://localhost:8081}")
    private String userServiceBaseUrl;

    @PostConstruct
    public void init() {
        try {
            if (resilientWebClientConfig != null) {
                this.webClient = resilientWebClientConfig.createResilientWebClient("user-service", userServiceBaseUrl);
                logger.info("UserServiceClient initialized with resilient WebClient, base URL: {}", userServiceBaseUrl);
            } else {
                // Fallback to simple WebClient if ResilientWebClientConfig is not available
                this.webClient = WebClient.builder()
                        .baseUrl(userServiceBaseUrl)
                        .build();
                logger.warn("UserServiceClient initialized with simple WebClient (ResilientWebClientConfig not available), base URL: {}", userServiceBaseUrl);
            }
        } catch (Exception e) {
            logger.error("Failed to initialize UserServiceClient with base URL: {}. Error: {}", 
                    userServiceBaseUrl, e.getMessage(), e);
            // Fallback to simple WebClient if resilient client fails
            this.webClient = WebClient.builder()
                    .baseUrl(userServiceBaseUrl)
                    .build();
            logger.warn("UserServiceClient initialized with fallback WebClient (no resilience features)");
        }
    }

    /**
     * Get user information by ID.
     * Returns null if user cannot be fetched (e.g., user not found, server error).
     * This allows callers to gracefully handle missing user information.
     */
    public UserDto getUser(UUID userId) {
        logger.info("Fetching user information: userId={}, baseUrl={}", userId, userServiceBaseUrl);

        if (webClient == null) {
            logger.error("WebClient is null! UserServiceClient was not properly initialized. baseUrl={}", userServiceBaseUrl);
            return null;
        }

        String fullUrl = userServiceBaseUrl + "/api/v1/users/" + userId;
        logger.debug("Calling user-service endpoint: {}", fullUrl);

        try {
            UserDto response = webClient.get()
                    .uri("/api/v1/users/{userId}", userId)
                    .retrieve()
                    .bodyToMono(UserDto.class)
                    .block();

            if (response != null) {
                logger.info("Successfully retrieved user information: userId={}, username={}, name={}, profilePictureUrl={}", 
                        userId, response.getUsername(), response.getName(), response.getProfilePictureUrl());
            } else {
                logger.warn("UserService returned null for userId={}. This might indicate the user doesn't exist in user-service database.", userId);
            }
            return response;
        } catch (WebClientResponseException.NotFound e) {
            logger.error("User not found in user-service: userId={}, URL={}, response body: {}", 
                    userId, fullUrl, e.getResponseBodyAsString());
            return null;
        } catch (WebClientResponseException e) {
            // Log but don't throw - allow graceful fallback
            logger.error("Error fetching user information for userId={}: status={}, URL={}, response body: {}. Will use default values.", 
                    userId, e.getStatusCode(), fullUrl, e.getResponseBodyAsString(), e);
            return null; // Return null to allow graceful fallback
        } catch (Exception e) {
            logger.error("Unexpected error fetching user information for userId={}, URL={}. Will use default values. Error type: {}, message: {}", 
                    userId, fullUrl, e.getClass().getSimpleName(), e.getMessage(), e);
            return null; // Return null to allow graceful fallback
        }
    }
}

