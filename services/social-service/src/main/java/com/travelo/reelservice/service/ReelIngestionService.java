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
     * Reel video already processed in media-service (filters, 9:16, optional music).
     */
    ReelDto ingestProcessedDelivery(String userId,
                                   UUID mediaId,
                                   String videoUrl,
                                   String thumbnailUrl,
                                   Integer durationSeconds,
                                   String caption,
                                   String location,
                                   String filterType,
                                   Boolean musicEnabled);
    
    /**
     * Update reel after transcoding is complete.
     */
    void updateReelAfterTranscoding(UUID reelId, String videoUrl, String thumbnailUrl, Integer durationSeconds);
}

