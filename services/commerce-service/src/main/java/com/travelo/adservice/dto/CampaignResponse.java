package com.travelo.adservice.dto;

import com.travelo.adservice.entity.CampaignStatus;
import com.travelo.adservice.entity.enums.BidStrategy;
import com.travelo.adservice.entity.enums.BudgetType;
import com.travelo.adservice.entity.enums.CampaignObjective;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Campaign response with all campaign details")
public class CampaignResponse {
    private UUID id;
    private UUID businessAccountId;
    private String name;
    private CampaignObjective objective;
    private CampaignStatus status;
    private Double budget;
    private BudgetType budgetType;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private BidStrategy bidStrategy;
    private Double bidAmount;
    private Double targetRoas;
    private Double targetCpa;
    private Double spend;
    private Long impressions;
    private Long clicks;
    private Double ctr;
    private Long conversions;
    private UUID createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static CampaignResponse fromEntity(com.travelo.adservice.entity.Campaign campaign) {
        CampaignResponse response = new CampaignResponse();
        response.setId(campaign.getId());
        response.setBusinessAccountId(campaign.getBusinessAccountId());
        response.setName(campaign.getName());
        response.setObjective(campaign.getObjective());
        response.setStatus(campaign.getStatus());
        response.setBudget(campaign.getBudget());
        response.setBudgetType(campaign.getBudgetType());
        response.setStartDate(campaign.getStartDate());
        response.setEndDate(campaign.getEndDate());
        response.setBidStrategy(campaign.getBidStrategy());
        response.setBidAmount(campaign.getBidAmount());
        response.setTargetRoas(campaign.getTargetRoas());
        response.setTargetCpa(campaign.getTargetCpa());
        response.setSpend(campaign.getSpend());
        response.setImpressions(campaign.getImpressions());
        response.setClicks(campaign.getClicks());
        response.setCtr(campaign.getCtr());
        response.setConversions(campaign.getConversions());
        response.setCreatedBy(campaign.getCreatedBy());
        response.setCreatedAt(campaign.getCreatedAt());
        response.setUpdatedAt(campaign.getUpdatedAt());
        return response;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CampaignObjective getObjective() {
        return objective;
    }

    public void setObjective(CampaignObjective objective) {
        this.objective = objective;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
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

    public BidStrategy getBidStrategy() {
        return bidStrategy;
    }

    public void setBidStrategy(BidStrategy bidStrategy) {
        this.bidStrategy = bidStrategy;
    }

    public Double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(Double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public Double getTargetRoas() {
        return targetRoas;
    }

    public void setTargetRoas(Double targetRoas) {
        this.targetRoas = targetRoas;
    }

    public Double getTargetCpa() {
        return targetCpa;
    }

    public void setTargetCpa(Double targetCpa) {
        this.targetCpa = targetCpa;
    }

    public Double getSpend() {
        return spend;
    }

    public void setSpend(Double spend) {
        this.spend = spend;
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

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
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

