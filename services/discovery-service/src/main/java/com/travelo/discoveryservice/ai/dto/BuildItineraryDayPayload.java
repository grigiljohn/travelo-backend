package com.travelo.discoveryservice.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BuildItineraryDayPayload(
        int dayNumber,
        String dayTitle,
        List<BuildItineraryItemPayload> items
) {
}
