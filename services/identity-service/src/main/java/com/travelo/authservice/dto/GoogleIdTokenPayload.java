package com.travelo.authservice.dto;

public class GoogleIdTokenPayload {

    private final String email;
    private final String name;
    private final boolean emailVerified;

    public GoogleIdTokenPayload(String email, String name, boolean emailVerified) {
        this.email = email;
        this.name = name;
        this.emailVerified = emailVerified;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }
}
