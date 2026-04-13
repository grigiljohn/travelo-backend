package com.travelo.adservice.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "campaign_labels", indexes = {
    @Index(name = "idx_campaign_id", columnList = "campaign_id"),
    @Index(name = "idx_label_id", columnList = "label_id"),
    @Index(name = "idx_unique", columnList = "campaign_id,label_id", unique = true)
})
public class CampaignLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "label_id", nullable = false)
    private UUID labelId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(UUID campaignId) {
        this.campaignId = campaignId;
    }

    public UUID getLabelId() {
        return labelId;
    }

    public void setLabelId(UUID labelId) {
        this.labelId = labelId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

