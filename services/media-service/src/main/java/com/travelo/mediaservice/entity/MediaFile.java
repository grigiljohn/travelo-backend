package com.travelo.mediaservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Media entity representing uploaded files with processing state, variants, and metadata.
 * Matches the spec database schema with UUID primary key, JSONB variants and meta fields.
 */
@Entity
@Table(name = "media")
public class MediaFile {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "owner_id", nullable = false, columnDefinition = "UUID")
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 16)
    private MediaType mediaType;

    @Column(name = "mime_type", length = 128)
    private String mimeType;

    @Column(name = "filename")
    private String filename;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "storage_bucket", nullable = false)
    private String storageBucket;

    @Column(name = "storage_etag")
    private String storageEtag;

    @Column(name = "upload_id")
    private String uploadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 32)
    private MediaStatus state = MediaStatus.UPLOAD_PENDING;

    @Column(name = "safety_status", length = 32)
    private String safetyStatus = "unknown";

    @Column(name = "variants", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<MediaVariant> variants = List.of();

    @Column(name = "meta", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> meta = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected MediaFile() {
        // For JPA
    }

    public MediaFile(UUID ownerId, MediaType mediaType, String mimeType, String filename, Long sizeBytes,
                     String storageKey, String storageBucket) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.mediaType = mediaType;
        this.mimeType = mimeType;
        this.filename = filename;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.storageBucket = storageBucket;
        this.state = MediaStatus.UPLOAD_PENDING;
        this.safetyStatus = "unknown";
    }

    @PrePersist
    void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.meta == null) {
            this.meta = new HashMap<>();
        }
        if (this.variants == null) {
            this.variants = List.of();
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getStorageBucket() {
        return storageBucket;
    }

    public void setStorageBucket(String storageBucket) {
        this.storageBucket = storageBucket;
    }

    public String getStorageEtag() {
        return storageEtag;
    }

    public void setStorageEtag(String storageEtag) {
        this.storageEtag = storageEtag;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public MediaStatus getState() {
        return state;
    }

    public void setState(MediaStatus state) {
        this.state = state;
    }

    public String getSafetyStatus() {
        return safetyStatus;
    }

    public void setSafetyStatus(String safetyStatus) {
        this.safetyStatus = safetyStatus;
    }

    public List<MediaVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<MediaVariant> variants) {
        this.variants = variants != null ? variants : List.of();
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta != null ? meta : new HashMap<>();
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "MediaFile{" +
                "id=" + id +
                ", ownerId=" + ownerId +
                ", mediaType=" + mediaType +
                ", mimeType='" + mimeType + '\'' +
                ", filename='" + filename + '\'' +
                ", sizeBytes=" + sizeBytes +
                ", storageKey='" + storageKey + '\'' +
                ", storageBucket='" + storageBucket + '\'' +
                ", storageEtag='" + storageEtag + '\'' +
                ", uploadId='" + uploadId + '\'' +
                ", state=" + state +
                ", safetyStatus='" + safetyStatus + '\'' +
                ", variants=" + variants +
                ", meta=" + meta +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
