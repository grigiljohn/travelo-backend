package com.travelo.planservice.dto;

import java.util.List;

public record AiGeneratePlanResponse(
        String title,
        String description,
        List<String> tags,
        String location,
        String timeSuggestion
) {
}
