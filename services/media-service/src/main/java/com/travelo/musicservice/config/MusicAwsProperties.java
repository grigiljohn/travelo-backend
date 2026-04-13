package com.travelo.musicservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "music.aws")
public class MusicAwsProperties {
    private String region = "ap-southeast-2";
    /** When true, S3Client uses cross-region access; S3Presigner always uses {@code region} only. */
    private boolean crossRegionAccessEnabled = true;
    private String bucket = "travelo-music-main";
    private String accountId = "unknown";
    private String uploadPrefix = "music";
    private String thumbnailPrefix = "music/thumbnails";
    private Integer presignExpiryMinutes = 60;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public boolean isCrossRegionAccessEnabled() {
        return crossRegionAccessEnabled;
    }

    public void setCrossRegionAccessEnabled(boolean crossRegionAccessEnabled) {
        this.crossRegionAccessEnabled = crossRegionAccessEnabled;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getUploadPrefix() {
        return uploadPrefix;
    }

    public void setUploadPrefix(String uploadPrefix) {
        this.uploadPrefix = uploadPrefix;
    }

    public String getThumbnailPrefix() {
        return thumbnailPrefix;
    }

    public void setThumbnailPrefix(String thumbnailPrefix) {
        this.thumbnailPrefix = thumbnailPrefix;
    }

    public Integer getPresignExpiryMinutes() {
        return presignExpiryMinutes;
    }

    public void setPresignExpiryMinutes(Integer presignExpiryMinutes) {
        this.presignExpiryMinutes = presignExpiryMinutes;
    }
}

