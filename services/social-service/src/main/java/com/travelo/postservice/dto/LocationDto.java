package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record LocationDto(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("display_name") String displayName,
    @JsonProperty("address") String address,
    @JsonProperty("latitude") BigDecimal latitude,
    @JsonProperty("longitude") BigDecimal longitude,
    @JsonProperty("place_id") String placeId,
    @JsonProperty("country_code") String countryCode,
    @JsonProperty("city") String city
) {}

