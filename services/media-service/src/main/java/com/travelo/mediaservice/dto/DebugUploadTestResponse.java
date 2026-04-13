package com.travelo.mediaservice.dto;

/**
 * Response for {@code POST /api/debug/upload-test} (storage / S3 smoke test).
 */
public record DebugUploadTestResponse(
        String status,
        String storageKey,
        boolean s3Enabled,
        String bucket,
        String region,
        long sizeBytes,
        String contentType,
        boolean existsAfterWrite,
        String message
) {
}
