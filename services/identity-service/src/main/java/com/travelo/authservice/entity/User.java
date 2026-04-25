package com.travelo.authservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_username", columnList = "username", unique = true),
    @Index(name = "idx_email", columnList = "email", unique = true)
})
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 50)
    private String name;
    
    @Column(nullable = false, unique = true, length = 30)
    private String username;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password; // Hashed with BCrypt
    
    @Column(length = 20)
    private String mobile;
    
    @Column(nullable = false)
    private Boolean isEmailVerified = false;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;
    
    private OffsetDateTime lastLoginAt;

    @Column(name = "profile_picture_url", length = 2048)
    private String profilePictureUrl;

    @Column(name = "cover_photo_url", length = 2048)
    private String coverPhotoUrl;

    @Column(length = 500)
    private String bio;

    @Column(name = "is_private", nullable = false)
    private Boolean isPrivate = Boolean.FALSE;

    @Column(name = "location_permission_granted", nullable = false)
    private Boolean locationPermissionGranted = Boolean.FALSE;

    @Column(name = "location_required", nullable = false)
    private Boolean locationRequired = Boolean.TRUE;

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(name = "current_location_label", length = 255)
    private String currentLocationLabel;

    @Column(name = "current_city", length = 120)
    private String currentCity;

    @Column(name = "current_country", length = 120)
    private String currentCountry;

    @Column(name = "current_location_updated_at")
    private OffsetDateTime currentLocationUpdatedAt;

    // Constructors
    public User() {
    }

    public User(String name, String username, String email, String password, String mobile) {
        this.name = name;
        this.username = username;
        this.email = email;
        this.password = password;
        this.mobile = mobile;
        this.isEmailVerified = false;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Boolean getIsEmailVerified() {
        return isEmailVerified;
    }

    public void setIsEmailVerified(Boolean isEmailVerified) {
        this.isEmailVerified = isEmailVerified;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(OffsetDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getCoverPhotoUrl() {
        return coverPhotoUrl;
    }

    public void setCoverPhotoUrl(String coverPhotoUrl) {
        this.coverPhotoUrl = coverPhotoUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate != null ? isPrivate : Boolean.FALSE;
    }

    public Boolean getLocationPermissionGranted() {
        return locationPermissionGranted;
    }

    public void setLocationPermissionGranted(Boolean locationPermissionGranted) {
        this.locationPermissionGranted = locationPermissionGranted != null ? locationPermissionGranted : Boolean.FALSE;
    }

    public Boolean getLocationRequired() {
        return locationRequired;
    }

    public void setLocationRequired(Boolean locationRequired) {
        this.locationRequired = locationRequired != null ? locationRequired : Boolean.TRUE;
    }

    public Double getCurrentLatitude() {
        return currentLatitude;
    }

    public void setCurrentLatitude(Double currentLatitude) {
        this.currentLatitude = currentLatitude;
    }

    public Double getCurrentLongitude() {
        return currentLongitude;
    }

    public void setCurrentLongitude(Double currentLongitude) {
        this.currentLongitude = currentLongitude;
    }

    public String getCurrentLocationLabel() {
        return currentLocationLabel;
    }

    public void setCurrentLocationLabel(String currentLocationLabel) {
        this.currentLocationLabel = currentLocationLabel;
    }

    public String getCurrentCity() {
        return currentCity;
    }

    public void setCurrentCity(String currentCity) {
        this.currentCity = currentCity;
    }

    public String getCurrentCountry() {
        return currentCountry;
    }

    public void setCurrentCountry(String currentCountry) {
        this.currentCountry = currentCountry;
    }

    public OffsetDateTime getCurrentLocationUpdatedAt() {
        return currentLocationUpdatedAt;
    }

    public void setCurrentLocationUpdatedAt(OffsetDateTime currentLocationUpdatedAt) {
        this.currentLocationUpdatedAt = currentLocationUpdatedAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", mobile='" + mobile + '\'' +
                ", isEmailVerified=" + isEmailVerified +
                ", isPrivate=" + isPrivate +
                ", locationPermissionGranted=" + locationPermissionGranted +
                ", locationRequired=" + locationRequired +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", lastLoginAt=" + lastLoginAt +
                '}';
    }
}

