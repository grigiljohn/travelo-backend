package com.travelo.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public class GoogleSignInRequestDTO {

    @NotBlank(message = "idToken is required")
    private String idToken;

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}
