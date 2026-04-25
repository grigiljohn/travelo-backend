package com.travelo.discoveryservice.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelSegmentPayload(
        String distance,
        String duration
) {
}
