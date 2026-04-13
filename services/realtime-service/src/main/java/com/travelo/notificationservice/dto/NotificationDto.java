package com.travelo.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class NotificationDto {
    
    @JsonProperty("id")
    private UUID id;
    
    @JsonProperty("user_id")
    private UUID userId;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("body")
    private String body;
    
    @JsonProperty("data")
    private Map<String, Object> data;
    
    @JsonProperty("actor_id")
    private UUID actorId;
    
    @JsonProperty("target_id")
    private UUID targetId;
    
    @JsonProperty("target_type")
    private String targetType;
    
    @JsonProperty("read")
    private Boolean read;
    
    @JsonProperty("read_at")
    private OffsetDateTime readAt;
    
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }
    public OffsetDateTime getReadAt() { return readAt; }
    public void setReadAt(OffsetDateTime readAt) { this.readAt = readAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}

