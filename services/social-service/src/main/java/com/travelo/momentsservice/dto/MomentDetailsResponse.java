package com.travelo.momentsservice.dto;

import java.util.List;

public record MomentDetailsResponse(
        String id,
        List<String> storedFiles,
        String mediaType,
        String caption,
        String location,
        List<String> tags,
        String thumbnailPath,
        Double trimStart,
        Double trimEnd,
        String videoFilter,
        String cropPreset,
        String musicUrl,
        String musicName,
        Double musicStart,
        boolean aiEnhanced,
        String segmentsJson,
        String highlightsJson,
        String scenesJson,
        String mediaDurationsJson
) {
}
