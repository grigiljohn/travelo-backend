package com.travelo.authservice.dto;

import java.util.UUID;

public class VerifyOtpResponseDTO {
    private UUID userId;
    private String email;
    private Boolean isEmailVerified;
    private String accessToken;
    private String refreshToken;

    public VerifyOtpResponseDTO() {
    }

    public VerifyOtpResponseDTO(UUID userId, String email, Boolean isEmailVerified, 
                               String accessToken, String refreshToken) {
        this.userId = userId;
        this.email = email;
        this.isEmailVerified = isEmailVerified;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getIsEmailVerified() {
        return isEmailVerified;
    }

    public void setIsEmailVerified(Boolean isEmailVerified) {
        this.isEmailVerified = isEmailVerified;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Override
    public String toString() {
        return "VerifyOtpResponseDTO{" +
                "userId=" + userId +
                ", email='" + email + '\'' +
                ", isEmailVerified=" + isEmailVerified +
                ", accessToken='***'" +
                ", refreshToken='***'" +
                '}';
    }
}

