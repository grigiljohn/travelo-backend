package com.travelo.mediaservice.service.impl;

import com.travelo.mediaservice.entity.MediaFile;
import com.travelo.mediaservice.entity.MediaStatus;
import com.travelo.mediaservice.exception.MediaFileNotFoundException;
import com.travelo.mediaservice.repository.MediaFileRepository;
import com.travelo.mediaservice.service.MediaModerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Content moderation stub for local storage.
 * AWS Rekognition (S3) has been removed. Mark all content as safe.
 * TODO: Integrate local moderation (e.g. TensorFlow model, external API).
 */
@Service
public class MediaModerationServiceImpl implements MediaModerationService {

    private static final Logger log = LoggerFactory.getLogger(MediaModerationServiceImpl.class);

    private final MediaFileRepository mediaFileRepository;

    public MediaModerationServiceImpl(MediaFileRepository mediaFileRepository) {
        this.mediaFileRepository = mediaFileRepository;
    }

    @Override
    @Transactional
    public void moderateMedia(UUID mediaId) {
        log.info("Running content moderation (stub) for mediaId={}", mediaId);
        MediaFile media = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new MediaFileNotFoundException(mediaId));
        media.setSafetyStatus("safe");
        Map<String, Object> moderationMeta = new HashMap<>();
        moderationMeta.put("confidence", 0.0);
        moderationMeta.put("reason", "Local storage - moderation skipped");
        moderationMeta.put("timestamp", java.time.Instant.now());
        media.getMeta().put("moderation", moderationMeta);
        mediaFileRepository.save(media);
    }

    @Override
    @Transactional(readOnly = true)
    public ModerationResult getModerationResult(UUID mediaId) {
        MediaFile media = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new MediaFileNotFoundException(mediaId));
        @SuppressWarnings("unchecked")
        Map<String, Object> meta = (Map<String, Object>) media.getMeta().get("moderation");
        if (meta == null) return null;
        return new ModerationResult(
                media.getSafetyStatus(),
                ((Number) meta.getOrDefault("confidence", 0)).doubleValue(),
                (String) meta.get("reason"),
                meta.get("raw_response")
        );
    }
}
