package com.travelo.discoveryservice.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiTripChatResponse(
        String assistantMessage,
        String destination,
        Integer durationDays,
        String budgetStyle,
        String interests,
        boolean complete,
        TripPlanDto tripPlan
) {}
