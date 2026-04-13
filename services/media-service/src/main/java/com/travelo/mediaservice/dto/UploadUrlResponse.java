package com.travelo.mediaservice.dto;

import java.util.List;
import java.util.UUID;

/**
 * Response for upload URL request.
 * Supports both single PUT and multipart upload methods.
 */
public record UploadUrlResponse(
        UUID mediaId,
        String uploadMethod,  // "s3_presigned_put" | "s3_multipart" | "tus" | "direct_server"
        Integer expiresIn,    // seconds
        String uploadUrl,     // presigned PUT URL (for single upload)
        String storageKey,
        String uploadId,      // multipart upload ID (for multipart)
        Long partSize,        // part size in bytes (for multipart)
        List<String> presignedPartUrls  // presigned URLs for each part (for multipart)
) {
    public static UploadUrlResponse singlePut(UUID mediaId, String uploadUrl, String storageKey, Integer expiresIn) {
        return new UploadUrlResponse(
                mediaId,
                "s3_presigned_put",
                expiresIn,
                uploadUrl,
                storageKey,
                null,
                null,
                null
        );
    }

    public static UploadUrlResponse multipart(UUID mediaId, String storageKey, String uploadId, Long partSize) {
        return new UploadUrlResponse(
                mediaId,
                "s3_multipart",
                null,
                null,
                storageKey,
                uploadId,
                partSize,
                null
        );
    }
}

