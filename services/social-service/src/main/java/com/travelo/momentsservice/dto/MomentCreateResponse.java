package com.travelo.momentsservice.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record MomentCreateResponse(
        String id,
        boolean success,
        String userId,
        String userName,
        String type,
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
        String mediaDurationsJson,
        String editorMetadataJson,
        String audience,
        List<String> storedFiles,
        OffsetDateTime createdAt
) {
}
