package com.travelo.postservice.exception;

public class PostNotFoundException extends RuntimeException {

    public PostNotFoundException(Long id) {
        super("Post %d not found".formatted(id));
    }

    public PostNotFoundException(String id) {
        super("Post %s not found".formatted(id));
    }
}

