package com.travelo.adservice.dto;

import com.travelo.adservice.entity.enums.BudgetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Map;

@Schema(description = "Request to create or update an ad group")
public record CreateAdGroupRequest(
    @Schema(description = "Ad group name", example = "Ad Set 1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Ad group name is required")
    String name,
    
    @Schema(description = "Ad group budget", example = "2000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Budget is required")
    @Positive(message = "Budget must be positive")
    Double budget,
    
    @Schema(description = "Budget type (DAILY or LIFETIME)", example = "DAILY", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Budget type is required")
    BudgetType budgetType,
    
    @Schema(description = "Targeting configuration (locations, demographics, interests, etc.)")
    Map<String, Object> targeting,
    
    @Schema(description = "List of keywords")
    List<String> keywords,
    
    @Schema(description = "List of negative keywords to exclude")
    List<String> negativeKeywords,
    
    @Schema(description = "List of target devices (mobile, desktop, tablet)")
    List<String> devices,
    
    @Schema(description = "List of ad placements")
    List<String> placements
) {
}

