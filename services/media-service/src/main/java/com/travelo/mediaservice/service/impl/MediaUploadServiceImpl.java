package com.travelo.mediaservice.service.impl;

import com.travelo.mediaservice.config.LocalStorageProperties;
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
import com.travelo.mediaservice.service.MediaDownloadUrlBuilder;
import com.travelo.mediaservice.service.MediaUploadService;
import com.travelo.mediaservice.util.LocalStorageKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MediaUploadServiceImpl implements MediaUploadService {

    private static final Logger log = LoggerFactory.getLogger(MediaUploadServiceImpl.class);

    private final MediaFileRepository mediaFileRepository;
    private final LocalStorageService localStorageService;
    private final LocalStorageProperties storageProperties;
    private final MediaKafkaProperties kafkaProperties;
    private final KafkaTemplate<String, MediaUploadedEvent> kafkaTemplate;
    private final MediaS3Properties mediaS3Properties;
    private final MediaDownloadUrlBuilder downloadUrlBuilder;

    public MediaUploadServiceImpl(MediaFileRepository mediaFileRepository,
                                  LocalStorageService localStorageService,
                                  LocalStorageProperties storageProperties,
                                  MediaKafkaProperties kafkaProperties,
                                  KafkaTemplate<String, MediaUploadedEvent> kafkaTemplate,
                                  MediaS3Properties mediaS3Properties,
                                  MediaDownloadUrlBuilder downloadUrlBuilder) {
        this.mediaFileRepository = mediaFileRepository;
        this.localStorageService = localStorageService;
        this.storageProperties = storageProperties;
        this.kafkaProperties = kafkaProperties;
        this.kafkaTemplate = kafkaTemplate;
        this.mediaS3Properties = mediaS3Properties;
        this.downloadUrlBuilder = downloadUrlBuilder;
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
            //kafkaTemplate.send(kafkaProperties.getTopic(), media.getId().toString(), event);
        } catch (Exception e) {
            log.warn("Failed to send Kafka event for mediaId={}", media.getId(), e);
        }

        String downloadUrl = downloadUrlBuilder.buildUploadDownloadUrl(media.getId(), storageKey);
        log.info(
                "flow=media_upload complete mediaId={} downloadUrl={} linkStyle={}",
                media.getId(),
                downloadUrl,
                downloadUrlBuilder.prefersPublicObjectUrls() ? "public_object" : "api");
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
        } catch (Exception e) {
            log.warn("Failed to send Kafka event for mediaId={}", mediaId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MediaFile getMedia(UUID mediaId) {
        return mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new MediaFileNotFoundException(mediaId));
    }

    @Override
    @Transactional(readOnly = true)
    public VariantsResponse getVariants(UUID mediaId, boolean includeSignedUrls) {
        MediaFile media = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new MediaFileNotFoundException(mediaId));

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

        String path;
        if (variant == null || variant.isEmpty()) {
            path = media.getStorageKey();
        } else {
            path = media.getVariants().stream()
                    .filter(v -> v != null && variant.equals(v.getName()))
                    .map(MediaVariant::getKey)
                    .filter(k -> k != null && !k.trim().isEmpty())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + variant));
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
