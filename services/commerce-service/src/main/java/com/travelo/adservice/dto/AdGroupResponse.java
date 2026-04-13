package com.travelo.adservice.dto;

import com.travelo.adservice.entity.AdGroupStatus;
import com.travelo.adservice.entity.enums.BudgetType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdGroupResponse {
    private UUID id;
    private UUID campaignId;
    private String name;
    private AdGroupStatus status;
    private Double budget;
    private BudgetType budgetType;
    private Map<String, Object> targeting;
    private List<String> keywords;
    private List<String> negativeKeywords;
    private List<String> devices;
    private List<String> placements;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static AdGroupResponse fromEntity(com.travelo.adservice.entity.AdGroup adGroup) {
        AdGroupResponse response = new AdGroupResponse();
        response.setId(adGroup.getId());
        response.setCampaignId(adGroup.getCampaignId());
        response.setName(adGroup.getName());
        response.setStatus(adGroup.getStatus());
        response.setBudget(adGroup.getBudget());
        response.setBudgetType(adGroup.getBudgetType());
        response.setTargeting(adGroup.getTargeting());
        response.setKeywords(adGroup.getKeywords());
        response.setNegativeKeywords(adGroup.getNegativeKeywords() != null ? List.of(adGroup.getNegativeKeywords()) : List.of());
        response.setDevices(adGroup.getDevices() != null ? List.of(adGroup.getDevices()) : List.of());
        response.setPlacements(adGroup.getPlacements() != null ? List.of(adGroup.getPlacements()) : List.of());
        response.setCreatedAt(adGroup.getCreatedAt());
        response.setUpdatedAt(adGroup.getUpdatedAt());
        return response;
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

    public List<String> getNegativeKeywords() {
        return negativeKeywords;
    }

    public void setNegativeKeywords(List<String> negativeKeywords) {
        this.negativeKeywords = negativeKeywords;
    }

    public List<String> getDevices() {
        return devices;
    }

    public void setDevices(List<String> devices) {
        this.devices = devices;
    }

    public List<String> getPlacements() {
        return placements;
    }

    public void setPlacements(List<String> placements) {
        this.placements = placements;
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

