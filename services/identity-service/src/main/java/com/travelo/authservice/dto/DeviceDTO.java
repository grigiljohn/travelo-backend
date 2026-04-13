package com.travelo.authservice.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class DeviceDTO {
    
    private UUID id;
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private Boolean isTrusted;
    private String ipAddress;
    private OffsetDateTime lastUsedAt;
    private OffsetDateTime createdAt;

    public DeviceDTO() {
    }

    public DeviceDTO(UUID id, String deviceId, String deviceName, String deviceType, 
                     Boolean isTrusted, String ipAddress, OffsetDateTime lastUsedAt, 
                     OffsetDateTime createdAt) {
        this.id = id;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.isTrusted = isTrusted;
        this.ipAddress = ipAddress;
        this.lastUsedAt = lastUsedAt;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public Boolean getIsTrusted() {
        return isTrusted;
    }

    public void setIsTrusted(Boolean isTrusted) {
        this.isTrusted = isTrusted;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public OffsetDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(OffsetDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "DeviceDTO{" +
                "id=" + id +
                ", deviceId='" + deviceId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", isTrusted=" + isTrusted +
                ", ipAddress='" + ipAddress + '\'' +
                ", lastUsedAt=" + lastUsedAt +
                ", createdAt=" + createdAt +
                '}';
    }
}

