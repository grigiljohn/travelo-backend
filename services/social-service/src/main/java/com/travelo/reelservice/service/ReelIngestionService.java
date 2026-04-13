package com.travelo.reelservice.service;

import com.travelo.reelservice.dto.CreateReelRequest;
import com.travelo.reelservice.dto.ReelDto;
import java.util.UUID;

/**
 * Service for reel ingestion - handles video upload and initial processing.
 */
public interface ReelIngestionService {
    
    /**
     * Create a new reel from uploaded video.
     * Triggers transcoding pipeline via Kafka event.
     */
    ReelDto createReel(String userId, CreateReelRequest request);
    
    /**
     * Update reel after transcoding is complete.
     */
    void updateReelAfterTranscoding(UUID reelId, String videoUrl, String thumbnailUrl, Integer durationSeconds);
}

