package com.travelo.mediaservice.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for rotating an image.
 * Rotation angle in degrees (0, 90, 180, 270, or any angle).
 */
public record RotateImageRequest(
    @NotNull(message = "Rotation angle is required")
    Double angleDegrees
) {
    public RotateImageRequest {
        if (angleDegrees != null && (angleDegrees < 0 || angleDegrees >= 360)) {
            throw new IllegalArgumentException("Rotation angle must be between 0 and 360 degrees");
        }
    }
}

