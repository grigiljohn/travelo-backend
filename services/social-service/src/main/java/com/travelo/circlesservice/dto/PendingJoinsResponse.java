package com.travelo.circlesservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PendingJoinsResponse {

    private List<String> pendingUserIds;

    public PendingJoinsResponse() {
    }

    public PendingJoinsResponse(List<String> pendingUserIds) {
        this.pendingUserIds = pendingUserIds;
    }

    public List<String> getPendingUserIds() {
        return pendingUserIds;
    }

    public void setPendingUserIds(List<String> pendingUserIds) {
        this.pendingUserIds = pendingUserIds;
    }
}
