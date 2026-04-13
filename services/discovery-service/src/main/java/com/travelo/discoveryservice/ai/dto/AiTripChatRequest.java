package com.travelo.discoveryservice.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Client sends the latest user message plus any slots already collected from prior turns.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiTripChatRequest(
        String lastUserMessage,
        String destination,
        Integer durationDays,
        String budgetStyle,
        String interests
) {}
