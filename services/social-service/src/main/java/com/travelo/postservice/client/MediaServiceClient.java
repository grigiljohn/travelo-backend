package com.travelo.postservice.client;

import com.travelo.postservice.client.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.travelo.commons.config.ResilientWebClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Client for interacting with media-service.
 * Handles all communication with the media microservice.
 */
@Component("postMediaServiceClient")
public class MediaServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(MediaServiceClient.class);

    private final WebClient webClient;

    public MediaServiceClient(
            ResilientWebClientConfig resilientWebClientConfig,
            @Value("${app.media-service.url:http://localhost:8084}") String mediaServiceBaseUrl) {
        this.webClient = resilientWebClientConfig.createResilientWebClient("media-service", mediaServiceBaseUrl);
        logger.info("MediaServiceClient initialized with base URL: {}", mediaServiceBaseUrl);
    }

    /**
     * Direct upload file to media-service (local storage).
     */
    public DirectUploadResponse uploadFile(MultipartFile file, UUID ownerId, String filename, String mediaType) {
        logger.debug("Uploading file to media-service: filename={}, size={}, mediaType={}",
                filename, file.getSize(), mediaType);
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("owner_id", ownerId.toString());
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            byte[] fileBytes;
            try {
                fileBytes = file.getBytes();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file: " + e.getMessage(), e);
            }
            builder.part("file", new ByteArrayResource(fileBytes)).contentType(MediaType.parseMediaType(contentType))
                    .filename(filename != null ? filename : (file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"));
            if (filename != null) builder.part("filename", filename);
            builder.part("media_type", mediaType != null ? mediaType : "image");

            DirectUploadResponse response = webClient.post()
                    .uri("/v1/media/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(builder.build())
                    .retrieve()
                    .bodyToMono(DirectUploadResponse.class)
                    .block();

            logger.info("File uploaded - mediaId={}, downloadUrl={}", response.mediaId(), response.downloadUrl());
            return response;
        } catch (WebClientResponseException e) {
            logger.error("Error uploading file to media-service: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    /**
     * Request presigned upload URL from media-service.
     * @deprecated Use uploadFile for local storage.
     */
    @Deprecated
    public UploadUrlResponse createUploadUrl(UUID ownerId, String filename, String mimeType, 
                                            Long sizeBytes, String mediaType, Boolean resumable) {
        logger.debug("Requesting upload URL from media-service: filename={}, size={}, mediaType={}", 
                filename, sizeBytes, mediaType);

        UploadUrlRequest request = new UploadUrlRequest(
                ownerId,
                filename,
                mimeType,
                sizeBytes,
                mapMediaType(mediaType),
                resumable,
                null
        );

        try {
            UploadUrlResponse response = webClient.post()
                    .uri("/v1/media/upload-url")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(UploadUrlResponse.class)
                    .block();

            logger.info("Received upload URL from media-service: mediaId={}, method={}", 
                    response.mediaId(), response.uploadMethod());
            return response;
        } catch (WebClientResponseException e) {
            logger.error("Error requesting upload URL from media-service: status={}, body={}", 
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to get upload URL from media-service: " + e.getMessage(), e);
        }
    }

    /**
     * Get presigned part URLs for multipart upload.
     */
    public MultipartPartUrlResponse getMultipartPartUrls(UUID mediaId, int partCount) {
        logger.debug("Requesting multipart part URLs: mediaId={}, partCount={}", mediaId, partCount);

        try {
            MultipartPartUrlResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/media/{mediaId}/multipart-urls")
                            .queryParam("parts", partCount)
                            .build(mediaId))
                    .retrieve()
                    .bodyToMono(MultipartPartUrlResponse.class)
                    .block();

            logger.debug("Received {} part URLs for mediaId={}", response.partUrls().size(), mediaId);
            return response;
        } catch (WebClientResponseException e) {
            logger.error("Error requesting multipart URLs: status={}", e.getStatusCode(), e);
            throw new RuntimeException("Failed to get multipart URLs: " + e.getMessage(), e);
        }
    }

    /**
     * Confirm upload completion.
     */
    public void completeUpload(UUID mediaId, String etag, Long sizeBytes) {
        logger.debug("Confirming upload: mediaId={}, etag={}, sizeBytes={}", mediaId, etag, sizeBytes);

        CompleteUploadRequest request = new CompleteUploadRequest(etag, sizeBytes);

        try {
            webClient.post()
                    .uri("/v1/media/{mediaId}/complete", mediaId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            logger.info("Upload confirmed for mediaId={}", mediaId);
        } catch (WebClientResponseException e) {
            logger.error("Error confirming upload: status={}", e.getStatusCode(), e);
            throw new RuntimeException("Failed to confirm upload: " + e.getMessage(), e);
        }
    }

    /**
     * Get media metadata.
     */
    public MediaFileResponse getMedia(UUID mediaId) {
        logger.debug("Fetching media metadata: mediaId={}", mediaId);

        try {
            MediaFileResponse response = webClient.get()
                    .uri("/v1/media/{mediaId}", mediaId)
                    .retrieve()
                    .bodyToMono(MediaFileResponse.class)
                    .block();

            logger.debug("Retrieved media metadata: mediaId={}, state={}", mediaId, response.state());
            return response;
        } catch (WebClientResponseException.NotFound e) {
            logger.warn("Media not found: mediaId={}", mediaId);
            return null;
        } catch (WebClientResponseException e) {
            logger.error("Error fetching media: status={}", e.getStatusCode(), e);
            throw new RuntimeException("Failed to fetch media: " + e.getMessage(), e);
        }
    }

    /**
     * Get variants for a media file.
     * Returns null if variants cannot be fetched (e.g., media not processed yet, server error).
     * This allows callers to gracefully fall back to download URL.
     */
    public VariantsResponse getVariants(UUID mediaId, boolean includeSignedUrls) {
        logger.debug("Fetching variants: mediaId={}, includeSignedUrls={}", mediaId, includeSignedUrls);

        try {
            VariantsResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/media/{mediaId}/variants")
                            .queryParam("includeSignedUrls", includeSignedUrls)
                            .build(mediaId))
                    .retrieve()
                    .bodyToMono(VariantsResponse.class)
                    .block();

            logger.debug("Retrieved {} variants for mediaId={}", 
                    response.variants() != null ? response.variants().size() : 0, mediaId);
            return response;
        } catch (WebClientResponseException e) {
            // Log but don't throw - allow fallback to download URL
            if (e.getStatusCode().is5xxServerError()) {
                logger.warn("Media service returned {} when fetching variants for mediaId={}. " +
                        "This may indicate media is still processing. Will fallback to download URL. Error: {}", 
                        e.getStatusCode(), mediaId, e.getMessage());
            } else if (e.getStatusCode().value() == 404) {
                logger.debug("Variants not found for mediaId={} (404). Media may not have variants yet. Will fallback to download URL.", 
                        mediaId);
            } else {
                logger.warn("Error fetching variants for mediaId={}: status={}. Will fallback to download URL. Error: {}", 
                        mediaId, e.getStatusCode(), e.getMessage());
            }
            return null; // Return null to allow graceful fallback
        } catch (Exception e) {
            logger.warn("Unexpected error fetching variants for mediaId={}. Will fallback to download URL. Error: {}", 
                    mediaId, e.getMessage());
            return null; // Return null to allow graceful fallback
        }
    }

    /**
     * Generate signed download URL.
     */
    public DownloadUrlResponse getDownloadUrl(UUID mediaId, String variant, Integer expiresInSeconds) {
        logger.debug("Generating download URL: mediaId={}, variant={}", mediaId, variant);

        try {
            DownloadUrlResponse response = webClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/v1/media/{mediaId}/download");
                        if (variant != null) {
                            builder.queryParam("variant", variant);
                        }
                        if (expiresInSeconds != null) {
                            builder.queryParam("expires", expiresInSeconds);
                        }
                        return builder.build(mediaId);
                    })
                    .retrieve()
                    .bodyToMono(DownloadUrlResponse.class)
                    .block();

            logger.debug("Generated download URL for mediaId={}", mediaId);
            return response;
        } catch (WebClientResponseException e) {
            logger.error("Error generating download URL: status={}", e.getStatusCode(), e);
            throw new RuntimeException("Failed to generate download URL: " + e.getMessage(), e);
        }
    }

    private String mapMediaType(String mediaType) {
        return switch (mediaType.toLowerCase()) {
            case "image" -> "IMAGE";
            case "video" -> "VIDEO";
            case "audio" -> "AUDIO";
            default -> "OTHER";
        };
    }
}

