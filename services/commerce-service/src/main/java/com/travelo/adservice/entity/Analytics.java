package com.travelo.adservice.entity;

import com.travelo.adservice.entity.enums.DeviceType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "analytics", indexes = {
    @Index(name = "idx_campaign_id", columnList = "campaign_id"),
    @Index(name = "idx_ad_group_id", columnList = "ad_group_id"),
    @Index(name = "idx_ad_id", columnList = "ad_id"),
    @Index(name = "idx_date", columnList = "date"),
    @Index(name = "idx_device", columnList = "device")
})
public class Analytics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "ad_group_id")
    private UUID adGroupId;

    @Column(name = "ad_id")
    private UUID adId;

    @Column(nullable = false)
    private OffsetDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DeviceType device;

    @Column(length = 100)
    private String location;

    @Column(nullable = false)
    private Long impressions = 0L;

    @Column(nullable = false)
    private Long clicks = 0L;

    @Column(nullable = false)
    private Double ctr = 0.0;

    @Column(nullable = false)
    private Double cpc = 0.0;

    @Column(nullable = false)
    private Double cpm = 0.0;

    @Column(nullable = false)
    private Double spend = 0.0;

    @Column(nullable = false)
    private Long conversions = 0L;

    @Column(name = "conversion_rate", nullable = false)
    private Double conversionRate = 0.0;

    @Column(nullable = false)
    private Double cpa = 0.0;

    @Column(nullable = false)
    private Double revenue = 0.0;

    @Column(nullable = false)
    private Double roas = 0.0;

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

    public UUID getAdId() {
        return adId;
    }

    public void setAdId(UUID adId) {
        this.adId = adId;
    }

    public OffsetDateTime getDate() {
        return date;
    }

    public void setDate(OffsetDateTime date) {
        this.date = date;
    }

    public DeviceType getDevice() {
        return device;
    }

    public void setDevice(DeviceType device) {
        this.device = device;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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

    public Double getCpm() {
        return cpm;
    }

    public void setCpm(Double cpm) {
        this.cpm = cpm;
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

    public Double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(Double conversionRate) {
        this.conversionRate = conversionRate;
    }

    public Double getCpa() {
        return cpa;
    }

    public void setCpa(Double cpa) {
        this.cpa = cpa;
    }

    public Double getRevenue() {
        return revenue;
    }

    public void setRevenue(Double revenue) {
        this.revenue = revenue;
    }

    public Double getRoas() {
        return roas;
    }

    public void setRoas(Double roas) {
        this.roas = roas;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

