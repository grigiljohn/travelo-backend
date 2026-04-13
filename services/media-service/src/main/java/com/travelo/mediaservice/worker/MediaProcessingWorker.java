package com.travelo.mediaservice.worker;

import com.travelo.mediaservice.event.MediaUploadedEvent;
import com.travelo.mediaservice.service.MediaProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kafka consumer worker that processes media.uploaded events.
 * Triggers the full processing pipeline (virus scan, moderation, transcoding, etc.)
 */
@Component
@ConditionalOnProperty(prefix = "media.kafka", name = "listeners-enabled", havingValue = "true")
public class MediaProcessingWorker {

    private static final Logger log = LoggerFactory.getLogger(MediaProcessingWorker.class);

    private final MediaProcessingService mediaProcessingService;

    public MediaProcessingWorker(MediaProcessingService mediaProcessingService) {
        this.mediaProcessingService = mediaProcessingService;
    }

    @KafkaListener(
            topics = "${media.kafka.topic:media.uploaded}",
            groupId = "${spring.application.name}-processor",
            containerFactory = "mediaKafkaListenerContainerFactory"
    )
    public void processMediaUploadedEvent(MediaUploadedEvent event) {
        log.info("Received media.uploaded event: mediaId={}, ownerId={}, storageKey={}",
                event.mediaId(), event.ownerId(), event.storageKey());

        try {
            UUID mediaId = UUID.fromString(event.mediaId());
            mediaProcessingService.processMedia(mediaId);
            log.info("Successfully processed mediaId={}", mediaId);
        } catch (Exception e) {
            log.error("Error processing media.uploaded event: mediaId={}", event.mediaId(), e);
            // TODO: Implement retry logic or dead letter queue
            throw e; // Will trigger Kafka retry
        }
    }
}

