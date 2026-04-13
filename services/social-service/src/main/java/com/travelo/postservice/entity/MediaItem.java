package com.travelo.postservice.entity;

import com.travelo.postservice.entity.enums.MediaType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "post_media_items")
public class MediaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "media_id", columnDefinition = "UUID")
    private java.util.UUID mediaId;  // Reference to media-service media ID

    @Column(length = 500)
    private String url;  // Cached URL for backward compatibility

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaType type;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    private Integer duration;

    private Integer width;

    private Integer height;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected MediaItem() {
        // for JPA
    }

    public MediaItem(Post post, String url, MediaType type, Integer position) {
        this.post = post;
        this.url = url;
        this.type = type;
        this.position = position;
    }

    public MediaItem(Post post, java.util.UUID mediaId, String url, MediaType type, Integer position) {
        this.post = post;
        this.mediaId = mediaId;
        this.url = url;
        this.type = type;
        this.position = position;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public String getPostId() {
        return post != null ? post.getId() : null;
    }

    public java.util.UUID getMediaId() {
        return mediaId;
    }

    public void setMediaId(java.util.UUID mediaId) {
        this.mediaId = mediaId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public MediaType getType() {
        return type;
    }

    public void setType(MediaType type) {
        this.type = type;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "MediaItem{" +
                "id=" + id +
                ", postId=" + (post != null ? post.getId() : null) +
                ", mediaId=" + mediaId +
                ", url='" + url + '\'' +
                ", type=" + type +
                ", position=" + position +
                ", thumbnailUrl='" + thumbnailUrl + '\'' +
                ", duration=" + duration +
                ", width=" + width +
                ", height=" + height +
                ", createdAt=" + createdAt +
                '}';
    }
}

