package com.travelo.mediaservice.service;

import java.util.UUID;

/**
 * Service for content moderation using ML providers (AWS Rekognition, Google Vision, etc.).
 * Updates safety_status field in media entity.
 */
public interface MediaModerationService {

    /**
     * Run content moderation on a media file.
     * Updates safety_status: safe | unsafe | review
     */
    void moderateMedia(UUID mediaId);

    /**
     * Get moderation result for a media file.
     */
    ModerationResult getModerationResult(UUID mediaId);

    record ModerationResult(String safetyStatus, Double confidence, String reason, Object rawResponse) {
        public static ModerationResult safe() {
            return new ModerationResult("safe", 1.0, null, null);
        }

        public static ModerationResult unsafe(Double confidence, String reason, Object rawResponse) {
            return new ModerationResult("unsafe", confidence, reason, rawResponse);
        }

        public static ModerationResult review(Double confidence, String reason, Object rawResponse) {
            return new ModerationResult("review", confidence, reason, rawResponse);
        }
    }
}
