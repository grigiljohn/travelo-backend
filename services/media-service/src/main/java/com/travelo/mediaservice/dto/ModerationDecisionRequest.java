package com.travelo.mediaservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class ModerationDecisionRequest {

    @NotBlank
    @JsonProperty("decision")
    private String decision; // approve | reject

    @JsonProperty("reason")
    private String reason;

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

