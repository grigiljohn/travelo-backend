package com.travelo.reelservice.exception;

import java.util.UUID;

public class ReelNotFoundException extends RuntimeException {
    public ReelNotFoundException(UUID reelId) {
        super("Reel not found: " + reelId);
    }
}

