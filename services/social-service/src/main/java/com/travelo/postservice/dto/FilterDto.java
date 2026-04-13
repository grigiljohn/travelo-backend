package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record FilterDto(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("type") String type,
    @JsonProperty("preview_url") String previewUrl,
    @JsonProperty("config") Map<String, Object> config,
    @JsonProperty("display_order") Integer displayOrder
) {}

