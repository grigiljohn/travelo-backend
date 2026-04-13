package com.travelo.aiorchestrator.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AutoCutTimelineRequest(
        @NotEmpty
        List<String> mediaIds,
        String templateId,
        Long targetDurationMs
) {
}

