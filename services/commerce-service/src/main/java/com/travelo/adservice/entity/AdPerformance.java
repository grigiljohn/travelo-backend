package com.travelo.adservice.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ad_performance", indexes = {
    @Index(name = "idx_ad_id", columnList = "ad_id"),
    @Index(name = "idx_date", columnList = "date")
})
public class AdPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_id", nullable = false)
    private Ad ad;

    @Column(nullable = false)
    private OffsetDateTime date;

    // Metrics
    @Column(name = "impressions", nullable = false)
    private Long impressions = 0L;

    @Column(name = "clicks", nullable = false)
    private Long clicks = 0L;

    @Column(name = "conversions", nullable = false)
    private Long conversions = 0L;

    @Column(name = "spend", nullable = false)
    private Double spend = 0.0;

    @Column(name = "revenue")
    private Double revenue = 0.0;

    @Column(name = "ctr")
    private Double ctr = 0.0; // Click-through rate

    @Column(name = "cpc")
    private Double cpc = 0.0; // Cost per click

    @Column(name = "cpm")
    private Double cpm = 0.0; // Cost per mille (1000 impressions)

    @Column(name = "conversion_rate")
    private Double conversionRate = 0.0;

    @Column(name = "roas")
    private Double roas = 0.0; // Return on ad spend

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
        if (this.date == null) {
            this.date = OffsetDateTime.now();
        }
        // Calculate derived metrics
        calculateMetrics();
    }

    @PreUpdate
    void onUpdate() {
        calculateMetrics();
    }

    private void calculateMetrics() {
        // Calculate CTR
        if (impressions > 0) {
            this.ctr = (clicks.doubleValue() / impressions.doubleValue()) * 100.0;
        }

        // Calculate CPC
        if (clicks > 0) {
            this.cpc = spend / clicks.doubleValue();
        }

        // Calculate CPM
        if (impressions > 0) {
            this.cpm = (spend / impressions.doubleValue()) * 1000.0;
        }

        // Calculate Conversion Rate
        if (clicks > 0) {
            this.conversionRate = (conversions.doubleValue() / clicks.doubleValue()) * 100.0;
        }

        // Calculate ROAS
        if (spend > 0) {
            this.roas = revenue / spend;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ad getAd() {
        return ad;
    }

    public void setAd(Ad ad) {
        this.ad = ad;
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

    public Long getConversions() {
        return conversions;
    }

    public void setConversions(Long conversions) {
        this.conversions = conversions;
    }

    public Double getSpend() {
        return spend;
    }

    public void setSpend(Double spend) {
        this.spend = spend;
    }

    public Double getRevenue() {
        return revenue;
    }

    public void setRevenue(Double revenue) {
        this.revenue = revenue;
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

    public Double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(Double conversionRate) {
        this.conversionRate = conversionRate;
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

