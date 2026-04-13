package com.travelo.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public class MarkAsReadRequest {
    
    @JsonProperty("notification_ids")
    private List<UUID> notificationIds;

    public List<UUID> getNotificationIds() { return notificationIds; }
    public void setNotificationIds(List<UUID> notificationIds) { this.notificationIds = notificationIds; }
}

