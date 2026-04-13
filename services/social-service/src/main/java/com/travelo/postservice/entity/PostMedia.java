package com.travelo.postservice.entity;

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
@Table(name = "post_media")
public class PostMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "media_id", columnDefinition = "UUID")
    private java.util.UUID mediaId;  // Reference to media-service media ID

    @Column(name = "media_url", length = 500)
    private String mediaUrl;  // Cached URL for backward compatibility

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;  // Cached thumbnail URL

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private MediaType mediaType;

    @Column(name = "order_index")
    private Integer orderIndex = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected PostMedia() {
        // for JPA
    }

    public PostMedia(java.util.UUID mediaId, String mediaUrl, String thumbnailUrl, MediaType mediaType, Integer orderIndex) {
        this.mediaId = mediaId;
        this.mediaUrl = mediaUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.mediaType = mediaType;
        if (orderIndex != null) {
            this.orderIndex = orderIndex;
        }
    }

    // Legacy constructor for backward compatibility
    public PostMedia(String mediaUrl, String thumbnailUrl, MediaType mediaType, Integer orderIndex) {
        this(null, mediaUrl, thumbnailUrl, mediaType, orderIndex);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public Long getId() {
        return id;
    }

    public Post getPost() {
        return post;
    }

    public java.util.UUID getMediaId() {
        return mediaId;
    }

    public void setMediaId(java.util.UUID mediaId) {
        this.mediaId = mediaId;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public enum MediaType {
        IMAGE,
        VIDEO
    }

    @Override
    public String toString() {
        return "PostMedia{" +
                "id=" + id +
                ", postId=" + (post != null ? post.getId() : null) +
                ", mediaId=" + mediaId +
                ", mediaUrl='" + mediaUrl + '\'' +
                ", thumbnailUrl='" + thumbnailUrl + '\'' +
                ", mediaType=" + mediaType +
                ", orderIndex=" + orderIndex +
                ", createdAt=" + createdAt +
                '}';
    }
}


