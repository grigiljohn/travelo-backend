package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for post templates used in AI-enhanced post creation
 */
@Schema(description = "Post template information")
public record PostTemplateDto(
        @JsonProperty("id")
        @Schema(description = "Template unique identifier", example = "edit")
        String id,

        @JsonProperty("label")
        @Schema(description = "Template display label", example = "Embrace l...")
        String label,

        @JsonProperty("icon")
        @Schema(description = "Icon name/identifier", example = "edit")
        String icon,

        @JsonProperty("thumbnail_url")
        @Schema(description = "Thumbnail image URL", nullable = true)
        String thumbnailUrl,

        @JsonProperty("is_default")
        @Schema(description = "Whether this is the default selected template", example = "false")
        Boolean isDefault,

        @JsonProperty("order")
        @Schema(description = "Display order", example = "2")
        Integer order
) {
}

