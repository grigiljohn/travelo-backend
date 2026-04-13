package com.travelo.postservice.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Service to handle media file uploads to S3 via media-service.
 * Internal service used by PostService to upload files before creating posts.
 */
public interface MediaUploadService {

    /**
     * Upload a file to S3 via media-service and return the media ID.
     * Handles the complete flow: get upload URL, upload to S3, confirm upload.
     * 
     * @param file The file to upload
     * @param ownerId The user ID who owns the media
     * @return The media ID from media-service
     */
    UUID uploadFile(MultipartFile file, UUID ownerId);
}

