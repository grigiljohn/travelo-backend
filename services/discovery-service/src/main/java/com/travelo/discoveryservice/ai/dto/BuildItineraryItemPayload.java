package com.travelo.discoveryservice.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BuildItineraryItemPayload(
        String time,
        String title,
        String description,
        String durationAtSpot,
        String icon,
        TravelSegmentPayload travelToNext
) {
}
