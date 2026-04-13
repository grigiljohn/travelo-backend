package com.travelo.postservice.entity;

import com.travelo.postservice.entity.enums.MediaType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "media_uploads", indexes = {
    @Index(name = "idx_media_uploads_user_id", columnList = "user_id"),
    @Index(name = "idx_media_uploads_created_at", columnList = "created_at")
})
public class MediaUpload {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(nullable = false, length = 500)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private MediaType mediaType;

    @Column(name = "file_size")
    private Long fileSize;

    private Integer width;

    private Integer height;

    private Integer duration; // For videos (seconds)

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected MediaUpload() {
        // for JPA
    }

    public MediaUpload(String id, String userId, String url, MediaType mediaType) {
        this.id = id;
        this.userId = userId;
        this.url = url;
        this.mediaType = mediaType;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "MediaUpload{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", url='" + url + '\'' +
                ", mediaType=" + mediaType +
                ", fileSize=" + fileSize +
                ", width=" + width +
                ", height=" + height +
                ", duration=" + duration +
                ", thumbnailUrl='" + thumbnailUrl + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

