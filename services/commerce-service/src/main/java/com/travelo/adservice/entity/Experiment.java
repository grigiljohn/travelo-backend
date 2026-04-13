package com.travelo.adservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "experiments", indexes = {
    @Index(name = "idx_base_campaign_id", columnList = "base_campaign_id"),
    @Index(name = "idx_status", columnList = "status")
})
public class Experiment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "base_campaign_id", nullable = false)
    private UUID baseCampaignId;

    @Column(name = "split_traffic", nullable = false)
    private Double splitTraffic = 50.0; // Percentage

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExperimentStatus status = ExperimentStatus.DRAFT;

    @Column(name = "start_date")
    private OffsetDateTime startDate;

    @Column(name = "end_date")
    private OffsetDateTime endDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> variations = new HashMap<>();

    @Column(name = "winner_campaign_id")
    private UUID winnerCampaignId;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getBaseCampaignId() {
        return baseCampaignId;
    }

    public void setBaseCampaignId(UUID baseCampaignId) {
        this.baseCampaignId = baseCampaignId;
    }

    public Double getSplitTraffic() {
        return splitTraffic;
    }

    public void setSplitTraffic(Double splitTraffic) {
        this.splitTraffic = splitTraffic;
    }

    public ExperimentStatus getStatus() {
        return status;
    }

    public void setStatus(ExperimentStatus status) {
        this.status = status;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(OffsetDateTime startDate) {
        this.startDate = startDate;
    }

    public OffsetDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(OffsetDateTime endDate) {
        this.endDate = endDate;
    }

    public Map<String, Object> getVariations() {
        return variations;
    }

    public void setVariations(Map<String, Object> variations) {
        this.variations = variations;
    }

    public UUID getWinnerCampaignId() {
        return winnerCampaignId;
    }

    public void setWinnerCampaignId(UUID winnerCampaignId) {
        this.winnerCampaignId = winnerCampaignId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}

