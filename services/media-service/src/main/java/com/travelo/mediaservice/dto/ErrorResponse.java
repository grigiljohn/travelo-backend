package com.travelo.mediaservice.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<ValidationError> errors
) {
    public record ValidationError(String field, String message) {
    }
}

