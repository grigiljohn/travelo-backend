package com.travelo.adservice.entity;

import com.travelo.adservice.entity.enums.BudgetType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "ad_groups", indexes = {
    @Index(name = "idx_campaign_id", columnList = "campaign_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_deleted_at", columnList = "deleted_at")
})
public class AdGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdGroupStatus status = AdGroupStatus.DRAFT;

    @Column(nullable = false)
    private Double budget;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_type", nullable = false, length = 20)
    private BudgetType budgetType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> targeting = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> keywords = new ArrayList<>();

    @Column(name = "negative_keywords", columnDefinition = "text[]")
    private String[] negativeKeywords = new String[0];

    @Column(columnDefinition = "varchar[]")
    private String[] devices = new String[0];

    @Column(columnDefinition = "varchar[]")
    private String[] placements = new String[0];

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @OneToMany(mappedBy = "adGroup", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Ad> ads = new ArrayList<>();

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

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public UUID getCampaignId() {
        return campaign != null ? campaign.getId() : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AdGroupStatus getStatus() {
        return status;
    }

    public void setStatus(AdGroupStatus status) {
        this.status = status;
    }

    public Double getBudget() {
        return budget;
    }

    public void setBudget(Double budget) {
        this.budget = budget;
    }

    public BudgetType getBudgetType() {
        return budgetType;
    }

    public void setBudgetType(BudgetType budgetType) {
        this.budgetType = budgetType;
    }

    public Map<String, Object> getTargeting() {
        return targeting;
    }

    public void setTargeting(Map<String, Object> targeting) {
        this.targeting = targeting;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String[] getNegativeKeywords() {
        return negativeKeywords;
    }

    public void setNegativeKeywords(String[] negativeKeywords) {
        this.negativeKeywords = negativeKeywords;
    }

    public String[] getDevices() {
        return devices;
    }

    public void setDevices(String[] devices) {
        this.devices = devices;
    }

    public String[] getPlacements() {
        return placements;
    }

    public void setPlacements(String[] placements) {
        this.placements = placements;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public List<Ad> getAds() {
        return ads;
    }

    public void setAds(List<Ad> ads) {
        this.ads = ads;
    }

    @Override
    public String toString() {
        return "AdGroup{" +
                "id=" + id +
                ", campaignId=" + (campaign != null ? campaign.getId() : null) +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", budget=" + budget +
                ", budgetType=" + budgetType +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", deletedAt=" + deletedAt +
                '}';
    }
}

