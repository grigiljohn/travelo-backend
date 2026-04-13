package com.travelo.storyservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.travelo.commons.config.ResilientWebClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

/**
 * Client to interact with media-service for story media uploads.
 */
@Component("storyMediaServiceClient")
public class MediaServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(MediaServiceClient.class);

    private final WebClient webClient;
    private final String mediaServiceUrl;

    public MediaServiceClient(ResilientWebClientConfig resilientWebClientConfig,
                             @Value("${app.media-service.url:http://localhost:8084}") String mediaServiceUrl) {
        this.mediaServiceUrl = mediaServiceUrl;
        this.webClient = resilientWebClientConfig.createResilientWebClient("media-service", mediaServiceUrl);
    }

    /**
     * Generate a download URL for media.
     */
    public String getDownloadUrl(UUID mediaId) {
        try {
            return webClient.get()
                    .uri("/v1/media/{mediaId}/download", mediaId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            logger.error("Error fetching download URL for media {}: {}", mediaId, e.getMessage());
            return null;
        }
    }
}

