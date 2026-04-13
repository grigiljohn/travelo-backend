package com.travelo.adservice.dto;

import com.travelo.adservice.entity.AdStatus;
import com.travelo.adservice.entity.enums.AdType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class AdResponse {
    private UUID id;
    private UUID adGroupId;
    private UUID creativeId;
    private String name;
    private AdStatus status;
    private AdType adType;
    private List<String> headlines;
    private List<String> descriptions;
    private String callToAction;
    private String finalUrl;
    private String displayUrl;
    private Integer qualityScore;
    private Integer adStrength;
    private Long impressions;
    private Long clicks;
    private Double ctr;
    private Long conversions;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static AdResponse fromEntity(com.travelo.adservice.entity.Ad ad) {
        AdResponse response = new AdResponse();
        response.setId(ad.getId());
        response.setAdGroupId(ad.getAdGroupId());
        response.setCreativeId(ad.getCreativeId());
        response.setName(ad.getName());
        response.setStatus(ad.getStatus());
        response.setAdType(ad.getAdType());
        response.setHeadlines(ad.getHeadlines() != null ? List.of(ad.getHeadlines()) : List.of());
        response.setDescriptions(ad.getDescriptions() != null ? List.of(ad.getDescriptions()) : List.of());
        response.setCallToAction(ad.getCallToAction());
        response.setFinalUrl(ad.getFinalUrl());
        response.setDisplayUrl(ad.getDisplayUrl());
        response.setQualityScore(ad.getQualityScore());
        response.setAdStrength(ad.getAdStrength());
        response.setImpressions(ad.getImpressions());
        response.setClicks(ad.getClicks());
        response.setCtr(ad.getCtr());
        response.setConversions(ad.getConversions());
        response.setCreatedAt(ad.getCreatedAt());
        response.setUpdatedAt(ad.getUpdatedAt());
        return response;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAdGroupId() {
        return adGroupId;
    }

    public void setAdGroupId(UUID adGroupId) {
        this.adGroupId = adGroupId;
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

    public List<String> getHeadlines() {
        return headlines;
    }

    public void setHeadlines(List<String> headlines) {
        this.headlines = headlines;
    }

    public List<String> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<String> descriptions) {
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

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

