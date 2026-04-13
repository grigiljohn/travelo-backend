package com.travelo.adservice.entity;

import com.travelo.adservice.entity.enums.BidStrategy;
import com.travelo.adservice.entity.enums.BudgetType;
import com.travelo.adservice.entity.enums.CampaignObjective;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "campaigns", indexes = {
    @Index(name = "idx_business_account_id", columnList = "business_account_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_deleted_at", columnList = "deleted_at")
})
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_account_id", nullable = false)
    private UUID businessAccountId;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CampaignObjective objective;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(nullable = false)
    private Double budget;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_type", nullable = false, length = 20)
    private BudgetType budgetType;

    @Column(name = "start_date")
    private OffsetDateTime startDate;

    @Column(name = "end_date")
    private OffsetDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "bid_strategy", length = 30)
    private BidStrategy bidStrategy;

    @Column(name = "bid_amount")
    private Double bidAmount;

    @Column(name = "target_roas")
    private Double targetRoas;

    @Column(name = "target_cpa")
    private Double targetCpa;

    @Column(length = 50)
    private String pacing; // standard, accelerated

    @Column(name = "optimization_goal", length = 50)
    private String optimizationGoal; // link_click, conversion, etc.

    @Column(name = "frequency_cap", length = 100)
    private String frequencyCap; // "2 impressions / 7 days"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> targeting = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> placements = new HashMap<>();

    @Column(nullable = false)
    private Double spend = 0.0;

    @Column(nullable = false)
    private Long impressions = 0L;

    @Column(nullable = false)
    private Long clicks = 0L;

    @Column(nullable = false)
    private Double ctr = 0.0;

    @Column(nullable = false)
    private Long conversions = 0L;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AdGroup> adGroups = new ArrayList<>();

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

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public List<AdGroup> getAdGroups() {
        return adGroups;
    }

    public void setAdGroups(List<AdGroup> adGroups) {
        this.adGroups = adGroups;
    }

    public String getPacing() {
        return pacing;
    }

    public void setPacing(String pacing) {
        this.pacing = pacing;
    }

    public String getOptimizationGoal() {
        return optimizationGoal;
    }

    public void setOptimizationGoal(String optimizationGoal) {
        this.optimizationGoal = optimizationGoal;
    }

    public String getFrequencyCap() {
        return frequencyCap;
    }

    public void setFrequencyCap(String frequencyCap) {
        this.frequencyCap = frequencyCap;
    }

    public Map<String, Object> getTargeting() {
        return targeting;
    }

    public void setTargeting(Map<String, Object> targeting) {
        this.targeting = targeting;
    }

    public Map<String, Object> getPlacements() {
        return placements;
    }

    public void setPlacements(Map<String, Object> placements) {
        this.placements = placements;
    }

    @Override
    public String toString() {
        return "Campaign{" +
                "id=" + id +
                ", businessAccountId=" + businessAccountId +
                ", name='" + name + '\'' +
                ", objective=" + objective +
                ", status=" + status +
                ", budget=" + budget +
                ", budgetType=" + budgetType +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", bidStrategy=" + bidStrategy +
                ", bidAmount=" + bidAmount +
                ", targetRoas=" + targetRoas +
                ", targetCpa=" + targetCpa +
                ", pacing='" + pacing + '\'' +
                ", optimizationGoal='" + optimizationGoal + '\'' +
                ", frequencyCap='" + frequencyCap + '\'' +
                ", spend=" + spend +
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
