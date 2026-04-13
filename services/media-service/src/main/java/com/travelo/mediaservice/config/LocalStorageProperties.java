package com.travelo.mediaservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for local file storage.
 * Replaces S3 for development and environments without cloud storage.
 */
@ConfigurationProperties(prefix = "media.storage.local")
public class LocalStorageProperties {

    /** Base directory for storing uploaded files (e.g. ./uploads or /var/travelo/media) */
    private String basePath = "./uploads";

    /** Base URL for serving files (e.g. http://localhost:8084) - used when generating download URLs */
    private String baseUrl = "http://localhost:8084";

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
