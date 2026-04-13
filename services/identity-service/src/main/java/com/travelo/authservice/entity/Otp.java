package com.travelo.authservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "otps", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_user_id", columnList = "user_id")
})
public class Otp {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false, length = 6)
    private String otp;
    
    @Column(nullable = false)
    private OffsetDateTime expiresAt;
    
    @Column(nullable = false)
    private Boolean isUsed = false;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Constructors
    public Otp() {
    }

    public Otp(UUID userId, String email, String otp, OffsetDateTime expiresAt) {
        this.userId = userId;
        this.email = email;
        this.otp = otp;
        this.expiresAt = expiresAt;
        this.isUsed = false;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getIsUsed() {
        return isUsed;
    }

    public void setIsUsed(Boolean isUsed) {
        this.isUsed = isUsed;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Otp{" +
                "id=" + id +
                ", userId=" + userId +
                ", email='" + email + '\'' +
                ", otp='" + otp + '\'' +
                ", expiresAt=" + expiresAt +
                ", isUsed=" + isUsed +
                ", createdAt=" + createdAt +
                '}';
    }
}

