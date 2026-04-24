package com.travelo.mediaservice.service;

import com.travelo.mediaservice.dto.*;
import com.travelo.mediaservice.entity.MediaFile;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling media uploads (local storage).
 */
public interface MediaUploadService {

    /**
     * Upload file directly to local storage.
     * @return DirectUploadResponse with mediaId and downloadUrl
     */
    DirectUploadResponse uploadFile(org.springframework.web.multipart.MultipartFile file,
                                   UUID ownerId,
                                   String filename,
                                    String mimeType,
                                   String mediaType);

    /**
     * Persist a fully processed reel MP4 (720×1280) without running the default transcode pipeline again.
     */
    DirectUploadResponse uploadReelProcessedDelivery(UUID ownerId, File processedMp4, String filename) throws java.io.IOException;

    /** Same as {@link #uploadFile} but from raw bytes (e.g. reel pipeline fallback). */
    DirectUploadResponse uploadRawBytes(UUID ownerId, byte[] data, String filename, String mimeType, String mediaType);

    /**
     * Generate presigned upload URL (single PUT or multipart).
     * @deprecated Use uploadFile for local storage instead.
     */
    @Deprecated
    UploadUrlResponse createUploadUrl(UploadUrlRequest request);

    /**
     * Generate presigned URLs for multipart upload parts.
     */
    MultipartPartUrlResponse generateMultipartPartUrls(UUID mediaId, int partCount);

    /**
     * Confirm upload completion and trigger processing.
     */
    void completeUpload(UUID mediaId, ConfirmUploadRequest request);

    /**
     * Get media file by ID.
     */
    MediaFile getMedia(UUID mediaId);

    /**
     * Get variants for a media file.
     */
    VariantsResponse getVariants(UUID mediaId, boolean includeSignedUrls);

    /**
     * Generate signed download URL for a media file or variant.
     */
    DownloadUrlResponse generateDownloadUrl(UUID mediaId, String variant, Integer expiresInSeconds);

    /**
     * Reprocess a media file (admin operation).
     */
    void reprocess(UUID mediaId, List<String> processingSteps);

    /**
     * Admin moderation queue.
     */
    List<MediaFile> getModerationQueue(int limit);

    /**
     * Admin moderation decision.
     * decision: approve | reject
     */
    MediaFile applyModerationDecision(UUID mediaId, String decision, String reason, String reviewer);
}
