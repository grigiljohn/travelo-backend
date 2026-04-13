package com.travelo.mediaservice.service;

import java.io.File;
import java.util.UUID;

/**
 * Service for generating thumbnails from video files.
 */
public interface ThumbnailService {
    
    /**
     * Generate thumbnail from video file.
     * 
     * @param videoFile The video file to generate thumbnail from
     * @param mediaId The media ID for naming the thumbnail
     * @return The generated thumbnail file, or null if generation failed
     */
    File generateThumbnail(File videoFile, UUID mediaId);
    
    /**
     * Generate thumbnail from video in local storage.
     * @param storageKey Local storage path of the video
     * @param mediaId The media ID for naming the thumbnail
     * @return Storage key of the saved thumbnail, or null if generation failed
     */
    String generateThumbnailFromLocal(String storageKey, UUID mediaId);
}

