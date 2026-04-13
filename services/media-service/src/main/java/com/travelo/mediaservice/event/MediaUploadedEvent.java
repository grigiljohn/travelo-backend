package com.travelo.mediaservice.event;

import java.time.Instant;
import java.util.Map;

/**
 * Event emitted after media upload passes virus scan (or after upload complete and scan job enqueued).
 * Matches the spec event structure.
 */
public record MediaUploadedEvent(
        String eventType,  // "media.uploaded"
        String mediaId,
        String ownerId,
        String storageBucket,
        String storageKey,
        String mimeType,
        Long sizeBytes,
        Instant timestamp,
        Map<String, String> callbacks  // optional webhook callbacks
) {
    // Convenience constructor with defaults
    public MediaUploadedEvent(String mediaId, String ownerId, String storageBucket, String storageKey,
                             String mimeType, Long sizeBytes, Instant timestamp) {
        this("media.uploaded", mediaId, ownerId, storageBucket, storageKey, mimeType, sizeBytes, timestamp, null);
    }

    public MediaUploadedEvent {
        if (eventType == null) {
            eventType = "media.uploaded";
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
