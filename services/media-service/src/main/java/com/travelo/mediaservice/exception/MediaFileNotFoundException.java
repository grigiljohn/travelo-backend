package com.travelo.mediaservice.exception;

import java.util.UUID;

public class MediaFileNotFoundException extends RuntimeException {

    public MediaFileNotFoundException(Long id) {
        super("Media file %d not found".formatted(id));
    }

    public MediaFileNotFoundException(UUID id) {
        super("Media file %s not found".formatted(id));
    }

    public MediaFileNotFoundException(String message) {
        super(message);
    }
}

