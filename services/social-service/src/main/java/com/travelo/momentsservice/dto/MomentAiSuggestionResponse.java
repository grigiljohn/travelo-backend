package com.travelo.momentsservice.dto;

import java.util.List;

public record MomentAiSuggestionResponse(
        String action,
        String caption,
        List<String> tags,
        String videoFilter,
        List<Double> scenes,
        String segmentsJson,
        String highlightsJson
) {
}
