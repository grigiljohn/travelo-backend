package com.travelo.messagingservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public class CreateConversationRequest {
    @JsonProperty("type")
    private String type; // "DIRECT" or "GROUP"
    @JsonProperty("participant_ids")
    private List<UUID> participantIds;
    @JsonProperty("name")
    private String name; // For group chats

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public List<UUID> getParticipantIds() { return participantIds; }
    public void setParticipantIds(List<UUID> participantIds) { this.participantIds = participantIds; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

