package com.travelo.messagingservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class ConversationDto {
    @JsonProperty("id")
    private UUID id;
    @JsonProperty("type")
    private String type;
    @JsonProperty("name")
    private String name;
    @JsonProperty("participants")
    private List<UUID> participants;
    @JsonProperty("last_message")
    private MessageDto lastMessage;
    @JsonProperty("unread_count")
    private Long unreadCount;
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
    
    // Additional fields for Flutter UI (participant info for DIRECT conversations)
    @JsonProperty("participant_id")
    private UUID participantId; // Other participant's ID (for DIRECT conversations)
    @JsonProperty("participant_name")
    private String participantName;
    @JsonProperty("participant_avatar_url")
    private String participantAvatarUrl;
    @JsonProperty("is_online")
    private Boolean isOnline;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<UUID> getParticipants() { return participants; }
    public void setParticipants(List<UUID> participants) { this.participants = participants; }
    public MessageDto getLastMessage() { return lastMessage; }
    public void setLastMessage(MessageDto lastMessage) { this.lastMessage = lastMessage; }
    public Long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(Long unreadCount) { this.unreadCount = unreadCount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public UUID getParticipantId() { return participantId; }
    public void setParticipantId(UUID participantId) { this.participantId = participantId; }
    public String getParticipantName() { return participantName; }
    public void setParticipantName(String participantName) { this.participantName = participantName; }
    public String getParticipantAvatarUrl() { return participantAvatarUrl; }
    public void setParticipantAvatarUrl(String participantAvatarUrl) { this.participantAvatarUrl = participantAvatarUrl; }
    public Boolean getIsOnline() { return isOnline; }
    public void setIsOnline(Boolean isOnline) { this.isOnline = isOnline; }

    @Override
    public String toString() {
        return "ConversationDto{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", participants=" + participants +
                ", lastMessage=" + lastMessage +
                ", unreadCount=" + unreadCount +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

