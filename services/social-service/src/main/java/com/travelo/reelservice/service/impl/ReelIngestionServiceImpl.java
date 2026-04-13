package com.travelo.reelservice.service.impl;

import com.travelo.reelservice.dto.CreateReelRequest;
import com.travelo.reelservice.dto.ReelDto;
import com.travelo.reelservice.entity.Reel;
import com.travelo.reelservice.repository.ReelRepository;
import com.travelo.reelservice.service.ReelIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(transactionManager = "reelTransactionManager")
public class ReelIngestionServiceImpl implements ReelIngestionService {

    private static final Logger logger = LoggerFactory.getLogger(ReelIngestionServiceImpl.class);

    private final ReelRepository reelRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ReelIngestionServiceImpl(ReelRepository reelRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.reelRepository = reelRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public ReelDto createReel(String userId, CreateReelRequest request) {
        logger.info("Creating reel for user {} with media {}", userId, request.getMediaId());

        // Check if reel already exists for this post
        if (request.getPostId() != null) {
            reelRepository.findByPostId(request.getPostId())
                    .ifPresent(reel -> {
                        throw new IllegalArgumentException("Reel already exists for post: " + request.getPostId());
                    });
        }

        Reel reel = new Reel(
                request.getPostId(),
                userId,
                request.getMediaId(),
                null, // videoUrl will be set after transcoding
                null, // thumbnailUrl will be set after transcoding
                request.getCaption()
        );
        reel.setLocation(request.getLocation());
        reel.setMusicTrack(request.getMusicTrack());
        reel.setStatus(Reel.Status.PENDING);
        reel.setTranscodingStatus(Reel.TranscodingStatus.PENDING);

        reel = reelRepository.save(reel);

        // Publish event to trigger transcoding
        publishTranscodingEvent(reel.getId(), request.getMediaId());

        logger.info("Created reel {} for user {}", reel.getId(), userId);
        return ReelDto.fromEntity(reel);
    }

    @Override
    public void updateReelAfterTranscoding(UUID reelId, String videoUrl, String thumbnailUrl, Integer durationSeconds) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new IllegalArgumentException("Reel not found: " + reelId));

        reel.setVideoUrl(videoUrl);
        reel.setThumbnailUrl(thumbnailUrl);
        reel.setDurationSeconds(durationSeconds);
        reel.setTranscodingStatus(Reel.TranscodingStatus.COMPLETED);
        reel.setStatus(Reel.Status.READY);

        reelRepository.save(reel);

        logger.info("Updated reel {} after transcoding", reelId);
    }

    private void publishTranscodingEvent(UUID reelId, UUID mediaId) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("reelId", reelId.toString());
            event.put("mediaId", mediaId.toString());
            event.put("eventType", "reel.transcoding.requested");
            event.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send("reel.transcoding.requested", reelId.toString(), event);
            logger.debug("Published transcoding event for reel {}", reelId);
        } catch (Exception e) {
            logger.error("Error publishing transcoding event: {}", e.getMessage());
        }
    }
}

