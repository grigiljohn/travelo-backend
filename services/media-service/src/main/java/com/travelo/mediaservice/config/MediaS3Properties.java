package com.travelo.mediaservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * S3 bucket for photo/video blobs when {@code media.storage.s3.enabled=true}.
 */
@ConfigurationProperties(prefix = "media.storage.s3")
public class MediaS3Properties {

    private boolean enabled = false;
    private String bucket = "";
    private String region = "ap-southeast-2";
    /** Prefix for object keys (no leading/trailing slashes). */
    private String keyPrefix = "media";
    private boolean crossRegionAccessEnabled = true;

    /**
     * If set (with S3 enabled), upload/processing responses use {@code this URL + full object key} instead of
     * {@code GET /v1/media/files/{id}}. Use a CloudFront distribution, S3 static website endpoint, or public
     * virtual-hosted bucket URL. Private buckets without a public edge still need API URLs — leave empty and set
     * {@code media.public-urls.api-base-url} instead.
     */
    private String publicObjectBaseUrl = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public boolean isCrossRegionAccessEnabled() {
        return crossRegionAccessEnabled;
    }

    public void setCrossRegionAccessEnabled(boolean crossRegionAccessEnabled) {
        this.crossRegionAccessEnabled = crossRegionAccessEnabled;
    }

    public String getPublicObjectBaseUrl() {
        return publicObjectBaseUrl;
    }

    public void setPublicObjectBaseUrl(String publicObjectBaseUrl) {
        this.publicObjectBaseUrl = publicObjectBaseUrl;
    }

    /** Value stored on {@link com.travelo.mediaservice.entity.MediaFile} and Kafka events (S3 bucket name or {@code local}). */
    public String getRecordedBucketName() {
        if (enabled && StringUtils.hasText(bucket)) {
            return bucket;
        }
        return "local";
    }
}
