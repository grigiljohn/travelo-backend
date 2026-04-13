package com.travelo.authservice.dto;

import java.util.UUID;

public class SignupResponseDTO {
    private UUID userId;
    private String email;
    private String username;
    private String name;
    private Boolean isEmailVerified;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Integer expiresIn;
    private Boolean needsOnboarding = Boolean.TRUE;

    public SignupResponseDTO() {
        this.tokenType = "Bearer";
    }

    public SignupResponseDTO(UUID userId, String email, String username, String name, 
                           Boolean isEmailVerified, String accessToken, String refreshToken, Integer expiresIn) {
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.name = name;
        this.isEmailVerified = isEmailVerified;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
    }

    // Getters and Setters
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Boolean getNeedsOnboarding() {
        return needsOnboarding;
    }

    public void setNeedsOnboarding(Boolean needsOnboarding) {
        this.needsOnboarding = needsOnboarding != null ? needsOnboarding : Boolean.TRUE;
    }

    @Override
    public String toString() {
        return "SignupResponseDTO{" +
                "userId=" + userId +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", name='" + name + '\'' +
                ", isEmailVerified=" + isEmailVerified +
                ", accessToken='***'" +
                ", refreshToken='***'" +
                ", tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                ", needsOnboarding=" + needsOnboarding +
                '}';
    }
}

