package com.travelo.collectionservice.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "collection_media", indexes = {
        @Index(name = "idx_collection_media_collection_sort", columnList = "collection_id,sort_order,created_at"),
        @Index(name = "idx_collection_media_captured_at", columnList = "collection_id,captured_at"),
        @Index(name = "idx_collection_media_geo", columnList = "collection_id,latitude,longitude")
})
public class CollectionMedia {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collection_id", nullable = false)
    private Collection collection;

    @Column(name = "media_url", nullable = false, length = 800)
    private String mediaUrl;

    @Column(name = "thumbnail_url", length = 800)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private CollectionMediaType mediaType = CollectionMediaType.IMAGE;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private CollectionMediaSourceType sourceType = CollectionMediaSourceType.DEVICE;

    @Column(name = "source_id", length = 120)
    private String sourceId;

    @Column(name = "captured_at")
    private OffsetDateTime capturedAt;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Collection getCollection() { return collection; }
    public void setCollection(Collection collection) { this.collection = collection; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public CollectionMediaType getMediaType() { return mediaType; }
    public void setMediaType(CollectionMediaType mediaType) { this.mediaType = mediaType; }
    public CollectionMediaSourceType getSourceType() { return sourceType; }
    public void setSourceType(CollectionMediaSourceType sourceType) { this.sourceType = sourceType; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public OffsetDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(OffsetDateTime capturedAt) { this.capturedAt = capturedAt; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
