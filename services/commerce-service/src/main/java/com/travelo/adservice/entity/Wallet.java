package com.travelo.adservice.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallets", indexes = {
    @Index(name = "idx_business_account_id", columnList = "business_account_id", unique = true)
})
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_account_id", nullable = false, unique = true)
    private UUID businessAccountId;

    @Column(nullable = false)
    private Double balance = 0.0;

    @Column(length = 10)
    private String currency = "USD";

    @Column(name = "auto_pay_enabled")
    private Boolean autoPayEnabled = false;

    @Column(name = "auto_pay_threshold")
    private Double autoPayThreshold;

    @Column(name = "auto_pay_payment_method_id")
    private UUID autoPayPaymentMethodId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBusinessAccountId() {
        return businessAccountId;
    }

    public void setBusinessAccountId(UUID businessAccountId) {
        this.businessAccountId = businessAccountId;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Boolean getAutoPayEnabled() {
        return autoPayEnabled;
    }

    public void setAutoPayEnabled(Boolean autoPayEnabled) {
        this.autoPayEnabled = autoPayEnabled;
    }

    public Double getAutoPayThreshold() {
        return autoPayThreshold;
    }

    public void setAutoPayThreshold(Double autoPayThreshold) {
        this.autoPayThreshold = autoPayThreshold;
    }

    public UUID getAutoPayPaymentMethodId() {
        return autoPayPaymentMethodId;
    }

    public void setAutoPayPaymentMethodId(UUID autoPayPaymentMethodId) {
        this.autoPayPaymentMethodId = autoPayPaymentMethodId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}

