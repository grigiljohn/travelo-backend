package com.travelo.adservice.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public class ErrorResponse {
    private String error;
    private String message;
    private Map<String, Object> details;
    private OffsetDateTime timestamp;

    public ErrorResponse() {
        this.timestamp = OffsetDateTime.now();
    }

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
        this.timestamp = OffsetDateTime.now();
    }

    public ErrorResponse(String error, String message, Map<String, Object> details) {
        this.error = error;
        this.message = message;
        this.details = details;
        this.timestamp = OffsetDateTime.now();
    }

    // Getters and Setters
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

