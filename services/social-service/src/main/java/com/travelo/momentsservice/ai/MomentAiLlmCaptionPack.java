package com.travelo.momentsservice.ai;

import java.util.List;

/**
 * LLM output merged into {@link com.travelo.momentsservice.dto.MomentAiSuggestionResponse} when valid.
 */
public record MomentAiLlmCaptionPack(
        String caption,
        List<String> tags,
        String videoFilter
) {
}
