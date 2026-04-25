package com.travelo.discoveryservice.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Structured itinerary for mobile trip planner. {@code source} is {@code openai} or {@code template}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BuildItineraryResponse(
        String title,
        String summary,
        String source,
        List<BuildItineraryDayPayload> days
) {
}
