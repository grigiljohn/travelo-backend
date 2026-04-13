package com.travelo.adservice.entity;

import com.travelo.adservice.entity.enums.AdType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ads", indexes = {
    @Index(name = "idx_ad_group_id", columnList = "ad_group_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_creative_id", columnList = "creative_id"),
    @Index(name = "idx_deleted_at", columnList = "deleted_at")
})
public class Ad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_group_id", nullable = false)
    private AdGroup adGroup;

    @Column(name = "creative_id")
    private UUID creativeId;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdStatus status = AdStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "ad_type", nullable = false, length = 30)
    private AdType adType;

    @Column(length = 255)
    private String headline; // Primary headline

    @Column(columnDefinition = "text[]")
    private String[] headlines = new String[0]; // Multiple headlines

    @Column(length = 1000)
    private String description; // Primary description

    @Column(columnDefinition = "text[]")
    private String[] descriptions = new String[0]; // Multiple descriptions

    @Column(name = "call_to_action", length = 50)
    private String callToAction;

    @Column(name = "cta_text", length = 50)
    private String ctaText; // CTA button text

    @Column(name = "brand_name", length = 255)
    private String brandName;

    @Column(name = "brand_website", length = 500)
    private String brandWebsite;

    @Column(name = "shop_id")
    private UUID shopId; // Link to shop when CTA = SHOP_NOW

    @Column(name = "product_id")
    private UUID productId; // Optional: specific product to promote

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "creative", columnDefinition = "jsonb")
    private Map<String, Object> creative = new HashMap<>();

    @Column(name = "final_url", length = 500)
    private String finalUrl;

    @Column(name = "display_url", length = 255)
    private String displayUrl;

    @Column(name = "quality_score")
    private Integer qualityScore;

    @Column(name = "ad_strength")
    private Integer adStrength;

    @Column(nullable = false)
    private Long impressions = 0L;

    @Column(nullable = false)
    private Long clicks = 0L;

    @Column(nullable = false)
    private Double ctr = 0.0;

    @Column(nullable = false)
    private Long conversions = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

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

    public AdGroup getAdGroup() {
        return adGroup;
    }

    public void setAdGroup(AdGroup adGroup) {
        this.adGroup = adGroup;
    }

    public UUID getAdGroupId() {
        return adGroup != null ? adGroup.getId() : null;
    }

    public UUID getCreativeId() {
        return creativeId;
    }

    public void setCreativeId(UUID creativeId) {
        this.creativeId = creativeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AdStatus getStatus() {
        return status;
    }

    public void setStatus(AdStatus status) {
        this.status = status;
    }

    public AdType getAdType() {
        return adType;
    }

    public void setAdType(AdType adType) {
        this.adType = adType;
    }

    public String[] getHeadlines() {
        return headlines;
    }

    public void setHeadlines(String[] headlines) {
        this.headlines = headlines;
    }

    public String[] getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(String[] descriptions) {
        this.descriptions = descriptions;
    }

    public String getCallToAction() {
        return callToAction;
    }

    public void setCallToAction(String callToAction) {
        this.callToAction = callToAction;
    }

    public String getFinalUrl() {
        return finalUrl;
    }

    public void setFinalUrl(String finalUrl) {
        this.finalUrl = finalUrl;
    }

    public String getDisplayUrl() {
        return displayUrl;
    }

    public void setDisplayUrl(String displayUrl) {
        this.displayUrl = displayUrl;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }

    public Integer getAdStrength() {
        return adStrength;
    }

    public void setAdStrength(Integer adStrength) {
        this.adStrength = adStrength;
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

    public Long getConversions() {
        return conversions;
    }

    public void setConversions(Long conversions) {
        this.conversions = conversions;
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

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCtaText() {
        return ctaText;
    }

    public void setCtaText(String ctaText) {
        this.ctaText = ctaText;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getBrandWebsite() {
        return brandWebsite;
    }

    public void setBrandWebsite(String brandWebsite) {
        this.brandWebsite = brandWebsite;
    }

    public UUID getShopId() {
        return shopId;
    }

    public void setShopId(UUID shopId) {
        this.shopId = shopId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public Map<String, Object> getCreative() {
        return creative;
    }

    public void setCreative(Map<String, Object> creative) {
        this.creative = creative;
    }

    @Override
    public String toString() {
        return "Ad{" +
                "id=" + id +
                ", adGroupId=" + (adGroup != null ? adGroup.getId() : null) +
                ", creativeId=" + creativeId +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", adType=" + adType +
                ", headline='" + headline + '\'' +
                ", description='" + description + '\'' +
                ", callToAction='" + callToAction + '\'' +
                ", ctaText='" + ctaText + '\'' +
                ", brandName='" + brandName + '\'' +
                ", brandWebsite='" + brandWebsite + '\'' +
                ", finalUrl='" + finalUrl + '\'' +
                ", displayUrl='" + displayUrl + '\'' +
                ", qualityScore=" + qualityScore +
                ", adStrength=" + adStrength +
                ", impressions=" + impressions +
                ", clicks=" + clicks +
                ", ctr=" + ctr +
                ", conversions=" + conversions +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", deletedAt=" + deletedAt +
                '}';
    }
}
