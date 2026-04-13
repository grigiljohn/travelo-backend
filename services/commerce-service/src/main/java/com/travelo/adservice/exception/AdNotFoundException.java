package com.travelo.adservice.exception;

public class AdNotFoundException extends RuntimeException {
    public AdNotFoundException(Long id) {
        super("Ad not found with id: " + id);
    }
}

