package com.travelo.storyservice.exception;

import java.util.UUID;

public class StoryNotFoundException extends RuntimeException {
    public StoryNotFoundException(UUID storyId) {
        super("Story not found: " + storyId);
    }
}

