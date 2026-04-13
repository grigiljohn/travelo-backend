package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TopicDto(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("description") String description,
    @JsonProperty("trend_score") BigDecimal trendScore,
    @JsonProperty("post_count") Integer postCount,
    @JsonProperty("is_active") Boolean isActive,
    @JsonProperty("created_at") OffsetDateTime createdAt
) {}

