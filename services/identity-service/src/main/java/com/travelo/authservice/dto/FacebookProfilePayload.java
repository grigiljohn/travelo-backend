package com.travelo.authservice.dto;

public class FacebookProfilePayload {

    private final String email;
    private final String name;

    public FacebookProfilePayload(String email, String name) {
        this.email = email;
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }
}
