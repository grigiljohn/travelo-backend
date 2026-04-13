package com.travelo.mediaservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for trimming a video.
 */
public record TrimVideoRequest(
    @NotNull(message = "Start time in seconds is required")
    @Min(value = 0, message = "Start time must be non-negative")
    Double startTimeSeconds,
    
    @NotNull(message = "End time in seconds is required")
    @Min(value = 0, message = "End time must be non-negative")
    Double endTimeSeconds
) {
    public TrimVideoRequest {
        if (endTimeSeconds != null && startTimeSeconds != null && endTimeSeconds <= startTimeSeconds) {
            throw new IllegalArgumentException("End time must be greater than start time");
        }
    }
}

