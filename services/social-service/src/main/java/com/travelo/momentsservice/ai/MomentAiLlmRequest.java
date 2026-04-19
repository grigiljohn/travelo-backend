package com.travelo.momentsservice.ai;

import java.util.List;

/**
 * Input for optional LLM caption/tag/filter enrichment (timeline stays heuristic).
 */
public record MomentAiLlmRequest(
        String normalizedAction,
        String userCaption,
        String location,
        List<String> parsedTags,
        double durationSec
) {
}
