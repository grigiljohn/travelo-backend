package com.travelo.postservice.controller;

import com.travelo.postservice.client.MediaServiceClient;
import com.travelo.postservice.client.dto.UploadUrlResponse;
import com.travelo.postservice.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Media controller that delegates to media-service.
 * Clients should use this to get presigned upload URLs.
 */
@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private static final Logger logger = LoggerFactory.getLogger(MediaController.class);
    private final MediaServiceClient mediaServiceClient;

    public MediaController(MediaServiceClient mediaServiceClient) {
        this.mediaServiceClient = mediaServiceClient;
        logger.info("MediaController initialized - delegating to media-service");
    }

    /**
     * Request presigned upload URL.
     * This delegates to media-service and returns the upload URL.
     * 
     * @param ownerId User ID who owns the media
     * @param filename Original filename
     * @param mimeType MIME type of the file
     * @param sizeBytes File size in bytes
     * @param mediaType Media type (image, video, audio, other)
     * @param resumable Whether to use resumable multipart upload (for large files)
     */
    @PostMapping("/upload-url")
    public ResponseEntity<ApiResponse<UploadUrlResponse>> requestUploadUrl(
            @RequestParam("owner_id") UUID ownerId,
            @RequestParam("filename") String filename,
            @RequestParam("mime_type") String mimeType,
            @RequestParam("size_bytes") Long sizeBytes,
            @RequestParam("media_type") String mediaType,
            @RequestParam(value = "resumable", required = false, defaultValue = "false") Boolean resumable) {

        logger.info("Requesting upload URL - ownerId={}, filename={}, size={}, mediaType={}, resumable={}",
                ownerId, filename, sizeBytes, mediaType, resumable);

        try {
            UploadUrlResponse response = mediaServiceClient.createUploadUrl(
                    ownerId, filename, mimeType, sizeBytes, mediaType, resumable);
            
            logger.info("Upload URL generated - mediaId={}, method={}", 
                    response.mediaId(), response.uploadMethod());
            
            return ResponseEntity.ok(
                    ApiResponse.success("Upload URL generated successfully", response)
            );
        } catch (Exception e) {
            logger.error("Error generating upload URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate upload URL: " + e.getMessage(), 
                            "UPLOAD_URL_ERROR"));
        }
    }

    /**
     * Get multipart part URLs for resumable uploads.
     */
    @GetMapping("/{mediaId}/multipart-urls")
    public ResponseEntity<ApiResponse<com.travelo.postservice.client.dto.MultipartPartUrlResponse>> getMultipartPartUrls(
            @PathVariable("mediaId") UUID mediaId,
            @RequestParam("parts") int parts) {

        logger.info("Requesting multipart URLs - mediaId={}, parts={}", mediaId, parts);

        try {
            com.travelo.postservice.client.dto.MultipartPartUrlResponse response = 
                    mediaServiceClient.getMultipartPartUrls(mediaId, parts);
            
            return ResponseEntity.ok(
                    ApiResponse.success("Multipart URLs generated successfully", response)
            );
        } catch (Exception e) {
            logger.error("Error generating multipart URLs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate multipart URLs: " + e.getMessage(), 
                            "MULTIPART_URL_ERROR"));
        }
    }

    /**
     * Confirm upload completion.
     */
    @PostMapping("/{mediaId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeUpload(
            @PathVariable("mediaId") UUID mediaId,
            @RequestParam("etag") String etag,
            @RequestParam("size_bytes") Long sizeBytes) {

        logger.info("Confirming upload - mediaId={}, etag={}, sizeBytes={}", mediaId, etag, sizeBytes);

        try {
            mediaServiceClient.completeUpload(mediaId, etag, sizeBytes);
            return ResponseEntity.accepted()
                    .body(ApiResponse.success("Upload confirmed", null));
        } catch (Exception e) {
            logger.error("Error confirming upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to confirm upload: " + e.getMessage(), 
                            "UPLOAD_CONFIRM_ERROR"));
        }
    }
}
