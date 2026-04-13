package com.travelo.postservice.service.impl;

import com.travelo.postservice.client.MediaServiceClient;
import com.travelo.postservice.client.dto.UploadUrlResponse;
import com.travelo.postservice.service.MediaUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * Service to handle media file uploads to S3 via media-service.
 * Uploads files directly to S3 using presigned URLs.
 */
@Service
public class MediaUploadServiceImpl implements MediaUploadService {

    private static final Logger logger = LoggerFactory.getLogger(MediaUploadServiceImpl.class);

    private final MediaServiceClient mediaServiceClient;
    private final WebClient webClient;

    public MediaUploadServiceImpl(MediaServiceClient mediaServiceClient, WebClient.Builder webClientBuilder) {
        this.mediaServiceClient = mediaServiceClient;
        this.webClient = webClientBuilder.build();
    }

    @Override
    public UUID uploadFile(MultipartFile file, UUID ownerId) {
        logger.info("Uploading file: filename={}, size={}, contentType={}", 
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        try {
            // Step 1: Get upload URL from media-service
            String mediaType = determineMediaType(file.getContentType());
            boolean resumable = file.getSize() > 10 * 1024 * 1024; // 10 MB threshold

            UploadUrlResponse uploadResponse = mediaServiceClient.createUploadUrl(
                    ownerId,
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload",
                    file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                    file.getSize(),
                    mediaType,
                    resumable
            );

            UUID mediaId = uploadResponse.mediaId();
            logger.debug("Received upload URL for mediaId={}, method={}", mediaId, uploadResponse.uploadMethod());

            // Step 2: Upload file directly to S3 using presigned URL
            if ("s3_presigned_put".equals(uploadResponse.uploadMethod())) {
                uploadToS3(file, uploadResponse.uploadUrl());
                logger.debug("File uploaded to S3 for mediaId={}", mediaId);

                // Step 3: Confirm upload
                // Use a placeholder ETag - media-service will verify via S3 HeadObject
                mediaServiceClient.completeUpload(mediaId, "server-upload", file.getSize());
                logger.debug("Upload confirmed for mediaId={}", mediaId);
            } else {
                throw new UnsupportedOperationException("Multipart upload not yet implemented in service layer");
            }

            logger.info("File uploaded successfully - mediaId={}", mediaId);
            return mediaId;

        } catch (Exception e) {
            logger.error("Error uploading file: filename={}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    private void uploadToS3(MultipartFile file, String presignedUrl) throws IOException {
        logger.debug("Uploading file to S3 presigned URL: {}, size={}, contentType={}", 
                presignedUrl, file.getSize(), file.getContentType());

        try {
            // S3 presigned PUT URLs: Content-Type and Content-Length were specified during URL generation
            // The presigned URL signature includes these values, so we must send them as headers
            // and they must match exactly what was used to generate the URL
            
            String contentType = file.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            byte[] fileBytes = file.getBytes();
            long contentLength = fileBytes.length;
            
            // Log what we're sending for debugging
            logger.debug("S3 upload - Content-Type: {}, Content-Length: {}", contentType, contentLength);
            
            // Upload with explicit headers that match what was signed
            // Use bodyValue with byte array - simpler and more reliable
            // IMPORTANT: Parse URI to avoid double-encoding of the presigned URL
            // Presigned URLs are already fully encoded, so we must parse them as-is
            URI uri = URI.create(presignedUrl);
            
            webClient.put()
                    .uri(uri)  // Use parsed URI instead of string to prevent re-encoding
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(contentLength)
                    .bodyValue(fileBytes)
                    .retrieve()
                    .toBodilessEntity()
                    .doOnError(error -> {
                        if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException we) {
                            logger.error("S3 upload failed: status={}, body={}", 
                                    we.getStatusCode(), we.getResponseBodyAsString());
                        } else {
                            logger.error("S3 upload error: {}", error.getMessage());
                        }
                    })
                    .block();

            logger.debug("File uploaded successfully to S3");
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            logger.error("Error uploading file to S3: presignedUrl={}, size={}, contentType={}, error={}", 
                    presignedUrl, file.getSize(), file.getContentType(), errorMsg, e);
            
            // Check if it's a WebClient error and try to extract response body
            if (e.getCause() != null) {
                logger.error("Root cause: {}", e.getCause().getMessage());
            }
            
            throw new IOException("Failed to upload file to S3: " + errorMsg, e);
        }
    }

    private String determineMediaType(String contentType) {
        if (contentType == null) {
            return "other";
        }
        if (contentType.startsWith("image/")) {
            return "image";
        } else if (contentType.startsWith("video/")) {
            return "video";
        } else if (contentType.startsWith("audio/")) {
            return "audio";
        }
        return "other";
    }
}
