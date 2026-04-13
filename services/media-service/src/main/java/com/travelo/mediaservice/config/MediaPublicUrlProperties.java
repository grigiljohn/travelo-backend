package com.travelo.mediaservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Client-facing URLs returned in API responses (upload, variants, download).
 * <p>
 * S3 stores blobs, but links often still go through this service's {@code GET /v1/media/files/{id}}.
 * Set {@code apiBaseUrl} to a host clients can reach (LB, gateway) instead of {@code localhost}.
 */
@ConfigurationProperties(prefix = "media.public-urls")
public class MediaPublicUrlProperties {

    /**
     * If non-empty, used as the base for API download links instead of {@code media.storage.local.base-url}.
     * Example: {@code https://api.example.com/media-service}
     */
    private String apiBaseUrl = "";

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }
}
