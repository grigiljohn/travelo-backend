package com.travelo.adservice.dto;

import com.travelo.adservice.entity.Asset;
import com.travelo.adservice.entity.enums.AssetType;
import com.travelo.adservice.entity.enums.StorageProvider;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AssetResponse {
    private UUID id;
    private String url;
    private String thumbnailUrl;
    private AssetType type;
    private Integer width;
    private Integer height;
    private Long size;
    private String format;
    private StorageProvider storageProvider;
    private UUID businessAccountId;
    private UUID uploadedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static AssetResponse fromEntity(Asset asset) {
        AssetResponse response = new AssetResponse();
        response.setId(asset.getId());
        response.setUrl(asset.getUrl());
        response.setThumbnailUrl(asset.getThumbnailUrl());
        response.setType(asset.getType());
        response.setWidth(asset.getWidth());
        response.setHeight(asset.getHeight());
        response.setSize(asset.getSize());
        response.setFormat(asset.getFormat());
        response.setStorageProvider(asset.getStorageProvider());
        response.setBusinessAccountId(asset.getBusinessAccountId());
        response.setUploadedBy(asset.getUploadedBy());
        response.setCreatedAt(asset.getCreatedAt());
        response.setUpdatedAt(asset.getUpdatedAt());
        return response;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public AssetType getType() {
        return type;
    }

    public void setType(AssetType type) {
        this.type = type;
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

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    public void setStorageProvider(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    public UUID getBusinessAccountId() {
        return businessAccountId;
    }

    public void setBusinessAccountId(UUID businessAccountId) {
        this.businessAccountId = businessAccountId;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(UUID uploadedBy) {
        this.uploadedBy = uploadedBy;
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
}

