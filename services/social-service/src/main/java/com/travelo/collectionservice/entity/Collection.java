package com.travelo.collectionservice.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "collections", indexes = {
        @Index(name = "idx_collections_user_type", columnList = "user_id,type,created_at"),
        @Index(name = "idx_collections_trip_id", columnList = "trip_id")
})
public class Collection {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(nullable = false, length = 140)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CollectionType type;

    @Column(name = "trip_id", length = 80)
    private String tripId;

    @Column(name = "cover_image_url", length = 600)
    private String coverImageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        final var now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public CollectionType getType() { return type; }
    public void setType(CollectionType type) { this.type = type; }
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
