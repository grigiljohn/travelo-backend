package com.travelo.postservice.client.dto;

import java.util.List;
import java.util.UUID;

public record VariantsResponse(
        UUID mediaId,
        String state,
        List<VariantInfo> variants
) {
    public record VariantInfo(
            String name,
            String key,
            String mime,
            Integer width,
            Integer height,
            Integer bitrate,
            Double duration,
            String signedUrl
    ) {
    }
}

