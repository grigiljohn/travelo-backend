package com.travelo.adservice.dto;

import com.travelo.adservice.entity.enums.AdType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@Schema(description = "Request to create or update an ad")
public record CreateAdRequest(
    @Schema(description = "Ad name", example = "Ad 1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Ad name is required")
    String name,
    
    @Schema(description = "Ad type (IMAGE, VIDEO, CAROUSEL, etc.)", example = "IMAGE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Ad type is required")
    AdType adType,
    
    @Schema(description = "Creative asset ID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID creativeId,
    
    @Schema(description = "List of ad headlines", example = "[\"Save up to 85% on Car Insurance\", \"Get the best deals\"]")
    List<String> headlines,
    
    @Schema(description = "List of ad descriptions", example = "[\"Get the best deals on car insurance\", \"Compare rates from top providers\"]")
    List<String> descriptions,
    
    @Schema(description = "Call to action text", example = "Learn More")
    String callToAction,
    
    @Schema(description = "Final URL where users will be directed", example = "https://example.com/landing")
    String finalUrl,
    
    @Schema(description = "Display URL shown in the ad", example = "example.com")
    String displayUrl
) {
}
