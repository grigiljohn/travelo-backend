package com.travelo.mediaservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for cropping an image.
 */
public record CropImageRequest(
    @NotNull(message = "X coordinate is required")
    @Min(value = 0, message = "X coordinate must be non-negative")
    Integer x,
    
    @NotNull(message = "Y coordinate is required")
    @Min(value = 0, message = "Y coordinate must be non-negative")
    Integer y,
    
    @NotNull(message = "Width is required")
    @Min(value = 1, message = "Width must be at least 1")
    Integer width,
    
    @NotNull(message = "Height is required")
    @Min(value = 1, message = "Height must be at least 1")
    Integer height
) {}

