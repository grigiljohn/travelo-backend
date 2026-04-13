package com.travelo.adservice.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "search_terms", indexes = {
    @Index(name = "idx_campaign_id", columnList = "campaign_id"),
    @Index(name = "idx_ad_group_id", columnList = "ad_group_id"),
    @Index(name = "idx_search_term", columnList = "search_term"),
    @Index(name = "idx_date", columnList = "date")
})
public class SearchTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "ad_group_id")
    private UUID adGroupId;

    @Column(name = "search_term", nullable = false, length = 500)
    private String searchTerm;

    @Column(nullable = false)
    private OffsetDateTime date;

    @Column(nullable = false)
    private Long impressions = 0L;

    @Column(nullable = false)
    private Long clicks = 0L;

    @Column(nullable = false)
    private Double ctr = 0.0;

    @Column(nullable = false)
    private Double cpc = 0.0;

    @Column(nullable = false)
    private Double spend = 0.0;

    @Column(nullable = false)
    private Long conversions = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
        if (this.date == null) {
            this.date = OffsetDateTime.now();
        }
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

    public UUID getAdGroupId() {
        return adGroupId;
    }

    public void setAdGroupId(UUID adGroupId) {
        this.adGroupId = adGroupId;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public OffsetDateTime getDate() {
        return date;
    }

    public void setDate(OffsetDateTime date) {
        this.date = date;
    }

    public Long getImpressions() {
        return impressions;
    }

    public void setImpressions(Long impressions) {
        this.impressions = impressions;
    }

    public Long getClicks() {
        return clicks;
    }

    public void setClicks(Long clicks) {
        this.clicks = clicks;
    }

    public Double getCtr() {
        return ctr;
    }

    public void setCtr(Double ctr) {
        this.ctr = ctr;
    }

    public Double getCpc() {
        return cpc;
    }

    public void setCpc(Double cpc) {
        this.cpc = cpc;
    }

    public Double getSpend() {
        return spend;
    }

    public void setSpend(Double spend) {
        this.spend = spend;
    }

    public Long getConversions() {
        return conversions;
    }

    public void setConversions(Long conversions) {
        this.conversions = conversions;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

