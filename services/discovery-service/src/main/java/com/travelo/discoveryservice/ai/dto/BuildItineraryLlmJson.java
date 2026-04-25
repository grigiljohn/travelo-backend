package com.travelo.discoveryservice.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * OpenAI output shape (no {@code source} field).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BuildItineraryLlmJson(
        String title,
        String summary,
        List<BuildItineraryDayPayload> days
) {
}
