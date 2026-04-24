package com.travelo.mediaservice.service.impl;

import com.travelo.mediaservice.config.MediaKafkaProperties;
import com.travelo.mediaservice.config.MediaS3Properties;
import com.travelo.mediaservice.dto.*;
import com.travelo.mediaservice.entity.MediaFile;
import com.travelo.mediaservice.entity.MediaStatus;
import com.travelo.mediaservice.entity.MediaVariant;
import com.travelo.mediaservice.event.MediaUploadedEvent;
import com.travelo.mediaservice.exception.MediaFileNotFoundException;
import com.travelo.mediaservice.repository.MediaFileRepository;
import com.travelo.mediaservice.service.LocalStorageService;
import com.travelo.mediaservice.service.MediaContentResolutionService;
import com.travelo.mediaservice.service.MediaDownloadUrlBuilder;
import com.travelo.mediaservice.service.MediaProcessingService;
import com.travelo.mediaservice.service.ThumbnailService;
import com.travelo.mediaservice.service.MediaUploadService;
import com.travelo.mediaservice.util.LocalStorageKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MediaUploadServiceImpl implements MediaUploadService {

    private static final Logger log = LoggerFactory.getLogger(MediaUploadServiceImpl.class);

    private final MediaFileRepository mediaFileRepository;
    private final LocalStorageService localStorageService;
    private final MediaKafkaProperties kafkaProperties;
    private final KafkaTemplate<String, MediaUploadedEvent> kafkaTemplate;
    private final ObjectProvider<MediaProcessingService> mediaProcessingServiceProvider;
    private final MediaS3Properties mediaS3Properties;
    private final MediaDownloadUrlBuilder downloadUrlBuilder;
    private final MediaContentResolutionService contentResolutionService;
    private final boolean enforceReadSafety;
    private final boolean requireReadyStateForServing;
    private final ThumbnailService thumbnailService;

    public MediaUploadServiceImpl(MediaFileRepository mediaFileRepository,
                                  LocalStorageService localStorageService,
                                  MediaKafkaProperties kafkaProperties,
                                  KafkaTemplate<String, MediaUploadedEvent> kafkaTemplate,
                                  ObjectProvider<MediaProcessingService> mediaProcessingServiceProvider,
                                  MediaS3Properties mediaS3Properties,
                                  MediaDownloadUrlBuilder downloadUrlBuilder,
                                  MediaContentResolutionService contentResolutionService,
                                  ThumbnailService thumbnailService,
                                  @org.springframework.beans.factory.annotation.Value("${media.moderation.enforce-read-safety:true}") boolean enforceReadSafety,
                                  @org.springframework.beans.factory.annotation.Value("${media.moderation.require-ready-state-for-serving:true}") boolean requireReadyStateForServing) {
        this.mediaFileRepository = mediaFileRepository;
        this.localStorageService = localStorageService;
        this.kafkaProperties = kafkaProperties;
        this.kafkaTemplate = kafkaTemplate;
        this.mediaProcessingServiceProvider = mediaProcessingServiceProvider;
        this.mediaS3Properties = mediaS3Properties;
        this.downloadUrlBuilder = downloadUrlBuilder;
        this.contentResolutionService = contentResolutionService;
        this.thumbnailService = thumbnailService;
        this.enforceReadSafety = enforceReadSafety;
        this.requireReadyStateForServing = requireReadyStateForServing;
    }

    @Override
    @Transactional
    public DirectUploadResponse uploadFile(MultipartFile file, UUID ownerId, String filename,
                                          String mimeType, String mediaType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Multipart part 'file' is required and must not be empty");
        }
        log.info(
                "flow=media_upload service START ownerId={} filename={} sizeBytes={} mediaTypeParam={} "
                        + "partContentType={} partOriginalName={} storageBackend={} recordedBucket={} s3Region={}",
                ownerId,
                filename,
                file.getSize(),
                mediaType,
                file.getContentType(),
                file.getOriginalFilename(),
                mediaS3Properties.isEnabled() ? "S3" : "local",
                mediaS3Properties.getRecordedBucketName(),
                mediaS3Properties.getRegion());

        UUID mediaId = UUID.randomUUID();
        String ext = extensionFromFilenameOrMime(filename != null ? filename : file.getOriginalFilename(), mimeType);
        String storageKey = LocalStorageKeyGenerator.rawOriginalKey(mediaId, ext);

        MediaFile media = new MediaFile(
                ownerId,
                mapMediaType(mediaType),
                mimeType != null ? mimeType : file.getContentType(),
                filename != null ? filename : file.getOriginalFilename(),
                file.getSize(),
                storageKey,
                mediaS3Properties.getRecordedBucketName()
        );
        // Entity constructor assigns a random id; storageKey + client URL use mediaId — must match DB row.
        media.setId(mediaId);
        media = mediaFileRepository.save(media);

        log.info("flow=media_upload persist row mediaId={} storageKey={} bucket={}",
                media.getId(), storageKey, media.getStorageBucket());
        localStorageService.save(storageKey, file);
        log.debug("flow=media_upload blob saved mediaId={} key={}", media.getId(), storageKey);
        media.setState(MediaStatus.PROCESSING);
        mediaFileRepository.save(media);

        dispatchMediaProcessing(media);

        String downloadUrl = downloadUrlBuilder.buildUploadDownloadUrl(media.getId(), storageKey);
        log.info(
                "flow=media_upload complete mediaId={} downloadUrl={} linkStyle={}",
                media.getId(),
                downloadUrl,
                downloadUrlBuilder.prefersPublicObjectUrls() ? "public_object" : "api");
        return new DirectUploadResponse(media.getId(), downloadUrl, storageKey);
    }

    @Override
    @Transactional
    public DirectUploadResponse uploadReelProcessedDelivery(UUID ownerId, File processedMp4, String filename) throws java.io.IOException {
        if (processedMp4 == null || !processedMp4.exists()) {
            throw new IllegalArgumentException("processedMp4 required");
        }
        byte[] bytes = Files.readAllBytes(processedMp4.toPath());
        UUID mediaId = UUID.randomUUID();
        String storageKey = LocalStorageKeyGenerator.rawOriginalKey(mediaId, ".mp4");
        String fname = filename != null ? filename : "reel_processed.mp4";

        MediaFile media = new MediaFile(
                ownerId,
                com.travelo.mediaservice.entity.MediaType.VIDEO,
                "video/mp4",
                fname,
                (long) bytes.length,
                storageKey,
                mediaS3Properties.getRecordedBucketName()
        );
        media.setId(mediaId);
        media.getMeta().put("reel_delivery", true);
        media = mediaFileRepository.save(media);
        localStorageService.save(storageKey, bytes, "video/mp4");

        String thumbKey = thumbnailService.generateThumbnailFromLocal(storageKey, mediaId);
        if (thumbKey != null) {
            List<MediaVariant> variants = new ArrayList<>();
            variants.add(new MediaVariant("thumbnail", thumbKey, "image/jpeg", 720, 1280));
            media.setVariants(variants);
            media.getMeta().put("thumbnail_key", thumbKey);
        }
        media.setState(MediaStatus.READY);
        mediaFileRepository.save(media);

        String downloadUrl = downloadUrlBuilder.buildUploadDownloadUrl(media.getId(), storageKey);
        log.info("flow=reel_delivery_upload mediaId={} bytes={}", media.getId(), bytes.length);
        return new DirectUploadResponse(media.getId(), downloadUrl, storageKey);
    }

    @Override
    @Transactional
    public DirectUploadResponse uploadRawBytes(UUID ownerId, byte[] data, String filename, String mimeType, String mediaType) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("data required");
        }
        UUID mediaId = UUID.randomUUID();
        String ext = extensionFromFilenameOrMime(filename, mimeType);
        String storageKey = LocalStorageKeyGenerator.rawOriginalKey(mediaId, ext);
        String fname = filename != null ? filename : "upload.bin";

        MediaFile media = new MediaFile(
                ownerId,
                mapMediaType(mediaType),
                mimeType != null ? mimeType : "application/octet-stream",
                fname,
                (long) data.length,
                storageKey,
                mediaS3Properties.getRecordedBucketName()
        );
        media.setId(mediaId);
        media = mediaFileRepository.save(media);
        localStorageService.save(storageKey, data, mimeType != null ? mimeType : "application/octet-stream");
        media.setState(MediaStatus.PROCESSING);
        mediaFileRepository.save(media);
        dispatchMediaProcessing(media);
        String downloadUrl = downloadUrlBuilder.buildUploadDownloadUrl(media.getId(), storageKey);
        return new DirectUploadResponse(media.getId(), downloadUrl, storageKey);
    }

    @Override
    @Deprecated
    @Transactional
    public UploadUrlResponse createUploadUrl(UploadUrlRequest request) {
        throw new UnsupportedOperationException("Presigned URLs not supported. Use POST /v1/media/upload for direct upload.");
    }

    @Override
    @Transactional(readOnly = true)
    public MultipartPartUrlResponse generateMultipartPartUrls(UUID mediaId, int partCount) {
        throw new UnsupportedOperationException("Multipart upload not supported. Use POST /v1/media/upload for direct upload.");
    }

    @Override
    @Transactional
    public void completeUpload(UUID mediaId, ConfirmUploadRequest request) {
        MediaFile media = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new MediaFileNotFoundException(mediaId));
        if (!localStorageService.exists(media.getStorageKey())) {
            throw new IllegalStateException("Uploaded file not found for mediaId=" + mediaId);
        }
        media.setStorageEtag(request.etag());
        media.setState(MediaStatus.PROCESSING);
        mediaFileRepository.save(media);

        dispatchMediaProcessing(media);
    }

    @Override
    @Transactional(readOnly = true)
    public MediaFile getMedia(UUID mediaId) {
        MediaFile media = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new MediaFileNotFoundException(mediaId));
        validateReadable(media, false);
        return media;
    }

    @Override
    @Transactional(readOnly = true)
    public VariantsResponse getVariants(UUID mediaId, boolean includeSignedUrls) {
        MediaFile media = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new MediaFileNotFoundException(mediaId));
        validateReadable(media, true);

        List<MediaVariant> variants = media.getVariants() != null ? media.getVariants() : List.of();
        List<VariantsResponse.VariantInfo> variantInfos = variants.stream()
                .map(VariantsResponse.VariantInfo::fromMediaVariant)
                .map(variant -> {
                    if (includeSignedUrls && variant.key() != null && !variant.key().trim().isEmpty()) {
                        String url = downloadUrlBuilder.buildVariantUrl(mediaId, variant.name(), variant.key());
                        return variant.withSignedUrl(url);
                    }
                    return variant;
                })
                .collect(Collectors.toList());

        return new VariantsResponse(
                media.getId(),
                media.getState() != null ? media.getState().name().toLowerCase() : "unknown",
                variantInfos
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadUrlResponse generateDownloadUrl(UUID mediaId, String variant, Integer expiresInSeconds) {
        MediaFile media = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new MediaFileNotFoundException(mediaId));
        validateReadable(media, true);

        MediaContentResolutionService.ResolvedContent resolved = contentResolutionService.resolve(media, variant);
        String path = resolved.storageKey();
        if (path == null) {
            throw new IllegalArgumentException("Variant not found: " + variant);
        }

        if (!localStorageService.exists(path)) {
            throw new MediaFileNotFoundException(
                    "Media content not available for id " + mediaId + " (object missing at storage key)");
        }

        String downloadUrl = downloadUrlBuilder.buildDownloadUrlForContent(
                mediaId,
                path,
                variant != null && !variant.isEmpty() ? variant : null);
        int expiry = expiresInSeconds != null ? expiresInSeconds : 3600;
        return new DownloadUrlResponse(downloadUrl, expiry);
    }

    private void validateReadable(MediaFile media, boolean strictState) {
        if (media == null) {
            return;
        }
        String safety = media.getSafetyStatus() == null ? "unknown" : media.getSafetyStatus().trim().toLowerCase(Locale.ROOT);
        if (enforceReadSafety && ("unsafe".equals(safety) || "review".equals(safety))) {
            throw new SecurityException("Media blocked by moderation policy");
        }
        if (strictState && requireReadyStateForServing) {
            if (media.getState() != MediaStatus.READY) {
                throw new IllegalStateException("Media is not ready for serving");
            }
        }
    }

    @Override
    @Transactional
    public void reprocess(UUID mediaId, List<String> processingSteps) {
        MediaFile media = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new MediaFileNotFoundException(mediaId));
        media.setState(MediaStatus.PROCESSING);
        mediaFileRepository.save(media);

        MediaUploadedEvent event = new MediaUploadedEvent(
                media.getId().toString(),
                media.getOwnerId().toString(),
                media.getStorageBucket(),
                media.getStorageKey(),
                media.getMimeType(),
                media.getSizeBytes(),
                Instant.now()
        );
        kafkaTemplate.send(kafkaProperties.getTopic(), media.getId().toString(), event);
    }

    private void dispatchMediaProcessing(MediaFile media) {
        boolean listenersEnabled = kafkaProperties.isListenersEnabled();
        if (listenersEnabled) {
            try {
                MediaUploadedEvent event = new MediaUploadedEvent(
                        media.getId().toString(),
                        media.getOwnerId().toString(),
                        media.getStorageBucket(),
                        media.getStorageKey(),
                        media.getMimeType(),
                        media.getSizeBytes(),
                        Instant.now()
                );
                kafkaTemplate.send(kafkaProperties.getTopic(), media.getId().toString(), event);
                log.info("flow=media_upload processing dispatched via kafka mediaId={}", media.getId());
                return;
            } catch (Exception e) {
                log.warn("Kafka dispatch failed for mediaId={}, falling back to inline processing", media.getId(), e);
            }
        }

        MediaProcessingService processingService = mediaProcessingServiceProvider.getIfAvailable();
        if (processingService == null) {
            log.warn("No MediaProcessingService available for inline processing mediaId={}", media.getId());
            return;
        }
        try {
            processingService.processMedia(media.getId());
            log.info("flow=media_upload processing completed inline mediaId={}", media.getId());
        } catch (Exception e) {
            log.error("Inline media processing failed mediaId={}", media.getId(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaFile> getModerationQueue(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        List<MediaFile> byState = mediaFileRepository.findByStateIn(List.of(MediaStatus.REVIEW, MediaStatus.UNSAFE));
        List<MediaFile> bySafety = mediaFileRepository.findBySafetyStatusIn(List.of("review", "unsafe"));
        java.util.Map<UUID, MediaFile> merged = new java.util.LinkedHashMap<>();
        for (MediaFile f : byState) {
            merged.put(f.getId(), f);
        }
        for (MediaFile f : bySafety) {
            merged.put(f.getId(), f);
        }
        return merged.values().stream()
                .sorted(Comparator.comparing(MediaFile::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .toList();
    }

    @Override
    @Transactional
    public MediaFile applyModerationDecision(UUID mediaId, String decision, String reason, String reviewer) {
        MediaFile media = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new MediaFileNotFoundException(mediaId));
        String d = decision == null ? "" : decision.trim().toLowerCase(Locale.ROOT);
        if (!"approve".equals(d) && !"reject".equals(d)) {
            throw new IllegalArgumentException("decision must be one of: approve, reject");
        }
        if ("approve".equals(d)) {
            media.setSafetyStatus("safe");
            if (media.getState() != MediaStatus.INFECTED) {
                media.setState(MediaStatus.READY);
            }
        } else {
            media.setSafetyStatus("unsafe");
            media.setState(MediaStatus.UNSAFE);
        }

        java.util.Map<String, Object> meta = media.getMeta() != null ? media.getMeta() : new HashMap<>();
        meta.put("moderation_manual", java.util.Map.of(
                "decision", d,
                "reason", reason == null ? "" : reason,
                "reviewer", reviewer == null ? "admin" : reviewer,
                "timestamp", OffsetDateTime.now().toString()
        ));
        media.setMeta(meta);
        return mediaFileRepository.save(media);
    }

    private static com.travelo.mediaservice.entity.MediaType mapMediaType(String mediaType) {
        if (mediaType == null) return com.travelo.mediaservice.entity.MediaType.OTHER;
        return switch (mediaType.toLowerCase()) {
            case "image" -> com.travelo.mediaservice.entity.MediaType.IMAGE;
            case "video" -> com.travelo.mediaservice.entity.MediaType.VIDEO;
            case "audio" -> com.travelo.mediaservice.entity.MediaType.AUDIO;
            default -> com.travelo.mediaservice.entity.MediaType.OTHER;
        };
    }

    private static String extensionFromFilenameOrMime(String filename, String mimeType) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.'));
        }
        if (mimeType != null) {
            return switch (mimeType.toLowerCase()) {
                case "image/jpeg", "image/jpg" -> ".jpg";
                case "image/png" -> ".png";
                case "image/gif" -> ".gif";
                case "image/webp" -> ".webp";
                case "video/mp4" -> ".mp4";
                case "video/quicktime" -> ".mov";
                default -> "";
            };
        }
        return "";
    }
}
