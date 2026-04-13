package com.travelo.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public class FacebookSignInRequestDTO {

    @NotBlank(message = "accessToken is required")
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
