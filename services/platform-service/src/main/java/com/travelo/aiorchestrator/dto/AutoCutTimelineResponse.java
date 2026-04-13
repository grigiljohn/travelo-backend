package com.travelo.aiorchestrator.dto;

import java.util.List;
import java.util.Map;

public record AutoCutTimelineResponse(
        List<String> mediaOrder,
        Map<String, Integer> durationsMs,
        List<Map<String, Object>> transitions,
        Map<String, String> labels
) {
}

