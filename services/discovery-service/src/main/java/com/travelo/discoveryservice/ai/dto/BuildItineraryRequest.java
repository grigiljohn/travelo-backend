package com.travelo.discoveryservice.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * User trip context for generating a day-by-day itinerary.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BuildItineraryRequest(
        String destination,
        Integer durationDays,
        String dateSummary,
        String budget,
        List<String> companions,
        List<String> activities,
        Integer customBudgetUsd
) {
}
