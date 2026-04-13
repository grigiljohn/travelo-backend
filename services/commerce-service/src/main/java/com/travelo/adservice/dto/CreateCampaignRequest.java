package com.travelo.adservice.dto;

import com.travelo.adservice.entity.enums.BidStrategy;
import com.travelo.adservice.entity.enums.BudgetType;
import com.travelo.adservice.entity.enums.CampaignObjective;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;

@Schema(description = "Request to create or update a campaign")
public record CreateCampaignRequest(
    @Schema(description = "Campaign name", example = "Summer Travel Campaign", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Campaign name is required")
    String name,
    
    @Schema(description = "Campaign objective", example = "SEARCH_ADS", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Campaign objective is required")
    CampaignObjective objective,
    
    @Schema(description = "Campaign budget", example = "5000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Budget is required")
    @Positive(message = "Budget must be positive")
    Double budget,
    
    @Schema(description = "Budget type (DAILY or LIFETIME)", example = "DAILY", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Budget type is required")
    BudgetType budgetType,
    
    @Schema(description = "Campaign start date (ISO 8601)", example = "2024-06-01T00:00:00Z")
    OffsetDateTime startDate,
    
    @Schema(description = "Campaign end date (ISO 8601)", example = "2024-08-31T23:59:59Z")
    OffsetDateTime endDate,
    
    @Schema(description = "Bidding strategy", example = "CPC")
    BidStrategy bidStrategy,
    
    @Schema(description = "Bid amount", example = "2.50")
    @Positive(message = "Bid amount must be positive")
    Double bidAmount,
    
    @Schema(description = "Target ROAS (Return on Ad Spend)", example = "6.0")
    @Positive(message = "Target ROAS must be positive")
    Double targetRoas,
    
    @Schema(description = "Target CPA (Cost Per Acquisition)", example = "50.00")
    @Positive(message = "Target CPA must be positive")
    Double targetCpa
) {
}
