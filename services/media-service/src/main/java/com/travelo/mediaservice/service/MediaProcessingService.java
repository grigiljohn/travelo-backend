package com.travelo.mediaservice.service;

import com.travelo.mediaservice.dto.CropImageRequest;
import com.travelo.mediaservice.dto.ProcessedMediaResponse;
import com.travelo.mediaservice.dto.RotateImageRequest;
import com.travelo.mediaservice.dto.TrimVideoRequest;

import java.util.List;
import java.util.UUID;

/**
 * Service for processing uploaded media files:
 * - Virus scanning
 * - Content moderation
 * - Transcoding (video/audio)
 * - Thumbnail generation
 * - EXIF stripping
 * - Face detection
 * - Video trimming
 * - Image cropping
 * - Image rotation
 */
public interface MediaProcessingService {

    /**
     * Process a media file after upload.
     * Orchestrates all processing steps.
     */
    void processMedia(UUID mediaId);

    /**
     * Process specific steps only (for reprocessing).
     */
    void processMedia(UUID mediaId, List<String> processingSteps);

    /**
     * Trim a video file.
     * Creates a new media file with the trimmed video.
     * 
     * @param mediaId The original video media ID
     * @param request Trim request with start and end times
     * @return Response with processed media ID and download URL
     */
    ProcessedMediaResponse trimVideo(UUID mediaId, TrimVideoRequest request);

    /**
     * Crop an image file.
     * Creates a new media file with the cropped image.
     * 
     * @param mediaId The original image media ID
     * @param request Crop request with coordinates and dimensions
     * @return Response with processed media ID and download URL
     */
    ProcessedMediaResponse cropImage(UUID mediaId, CropImageRequest request);

    /**
     * Rotate an image file.
     * Creates a new media file with the rotated image.
     * 
     * @param mediaId The original image media ID
     * @param request Rotate request with angle in degrees
     * @return Response with processed media ID and download URL
     */
    ProcessedMediaResponse rotateImage(UUID mediaId, RotateImageRequest request);
}

