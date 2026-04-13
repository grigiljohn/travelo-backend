package com.travelo.reelservice.event;

import com.travelo.reelservice.repository.ReelRepository;
import com.travelo.reelservice.service.ReelIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer for media transcoding events.
 * Listens for transcoding completion events from media-service.
 */
@Component
@ConditionalOnProperty(prefix = "reel.kafka", name = "listeners-enabled", havingValue = "true")
public class TranscodingEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TranscodingEventConsumer.class);

    private final ReelIngestionService reelIngestionService;
    private final ReelRepository reelRepository;

    public TranscodingEventConsumer(ReelIngestionService reelIngestionService, ReelRepository reelRepository) {
        this.reelIngestionService = reelIngestionService;
        this.reelRepository = reelRepository;
    }

    /**
     * Handle media transcoded event from media-service.
     * Topic: media.transcoded
     */
    @KafkaListener(topics = "media.transcoded", groupId = "reel-service-group")
    public void handleMediaTranscoded(Map<String, Object> event) {
        try {
            logger.info("Received media.transcoded event: {}", event);
            
            String mediaIdStr = (String) event.get("mediaId");
            String videoUrl = (String) event.get("videoUrl");
            String thumbnailUrl = (String) event.get("thumbnailUrl");
            Object durationObj = event.get("durationSeconds");
            
            if (mediaIdStr == null) {
                logger.warn("Invalid media.transcoded event: missing mediaId");
                return;
            }

            UUID mediaId = UUID.fromString(mediaIdStr);
            Integer durationSeconds = durationObj != null ? (Integer) durationObj : null;

            // Find reel by mediaId and update
            reelRepository.findByMediaId(mediaId).ifPresent(reel -> {
                reelIngestionService.updateReelAfterTranscoding(reel.getId(), videoUrl, thumbnailUrl, durationSeconds);
                logger.info("Updated reel {} after transcoding", reel.getId());
            });
            
            if (!reelRepository.findByMediaId(mediaId).isPresent()) {
                logger.warn("No reel found for mediaId: {}", mediaId);
            }

        } catch (Exception e) {
            logger.error("Error handling media.transcoded event: {}", e.getMessage(), e);
        }
    }
}

