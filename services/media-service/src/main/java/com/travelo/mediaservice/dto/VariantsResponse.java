package com.travelo.mediaservice.dto;

import com.travelo.mediaservice.entity.MediaVariant;

import java.util.List;
import java.util.UUID;

/**
 * Response for GET /v1/media/{media_id}/variants
 */
public record VariantsResponse(
        UUID mediaId,
        String state,  // processing | ready | unsafe | review
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
            String signedUrl  // presigned download URL (optional, if requested)
    ) {
        public static VariantInfo fromMediaVariant(MediaVariant variant) {
            return new VariantInfo(
                    variant.getName(),
                    variant.getKey(),
                    variant.getMime(),
                    variant.getWidth(),
                    variant.getHeight(),
                    variant.getBitrate(),
                    variant.getDuration(),
                    null  // signedUrl generated separately
            );
        }

        public VariantInfo withSignedUrl(String signedUrl) {
            return new VariantInfo(
                    name, key, mime, width, height, bitrate, duration, signedUrl
            );
        }
    }
}

