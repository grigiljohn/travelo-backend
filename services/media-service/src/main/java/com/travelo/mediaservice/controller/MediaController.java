package com.travelo.mediaservice.controller;

import com.travelo.mediaservice.dto.*;
import com.travelo.mediaservice.entity.MediaFile;
import com.travelo.mediaservice.reel.ReelFilterType;
import com.travelo.mediaservice.reel.ReelCapabilitiesService;
import com.travelo.mediaservice.reel.ReelJobProgressBroker;
import com.travelo.mediaservice.reel.ReelJobProgressTracker;
import com.travelo.mediaservice.reel.ReelJobStage;
import com.travelo.mediaservice.service.MediaUploadService;
import com.travelo.mediaservice.service.ReelVideoPipelineService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * REST controller for media upload, download, and variant management.
 * Blobs are stored via {@link com.travelo.mediaservice.service.LocalStorageService} (local disk or S3).
 * {@code downloadUrl} in responses is built by {@link com.travelo.mediaservice.service.MediaDownloadUrlBuilder}.
 */
@RestController
@RequestMapping("/v1/media")
public class MediaController {

    private static final Logger log = LoggerFactory.getLogger(MediaController.class);

    private final MediaUploadService mediaUploadService;
    private final com.travelo.mediaservice.service.MediaProcessingService mediaProcessingService;
    private final com.travelo.mediaservice.service.LocalStorageService localStorageService;
    private final com.travelo.mediaservice.service.MediaContentResolutionService contentResolutionService;
    private final ReelVideoPipelineService reelVideoPipelineService;
    private final ReelCapabilitiesService reelCapabilitiesService;
    private final ReelJobProgressTracker reelJobProgressTracker;
    private final ReelJobProgressBroker reelJobProgressBroker;
    private final boolean enforceReadSafety;
    private final boolean requireReadyStateForServing;
    private final String moderationAdminToken;

    public MediaController(MediaUploadService mediaUploadService,
                          com.travelo.mediaservice.service.MediaProcessingService mediaProcessingService,
                          com.travelo.mediaservice.service.LocalStorageService localStorageService,
                          com.travelo.mediaservice.service.MediaContentResolutionService contentResolutionService,
                          ReelVideoPipelineService reelVideoPipelineService,
                          ReelCapabilitiesService reelCapabilitiesService,
                          ReelJobProgressTracker reelJobProgressTracker,
                          ReelJobProgressBroker reelJobProgressBroker,
                          @org.springframework.beans.factory.annotation.Value("${media.moderation.enforce-read-safety:true}") boolean enforceReadSafety,
                          @org.springframework.beans.factory.annotation.Value("${media.moderation.require-ready-state-for-serving:true}") boolean requireReadyStateForServing,
                          @org.springframework.beans.factory.annotation.Value("${media.moderation.admin-token:}") String moderationAdminToken) {
        this.mediaUploadService = mediaUploadService;
        this.mediaProcessingService = mediaProcessingService;
        this.localStorageService = localStorageService;
        this.contentResolutionService = contentResolutionService;
        this.reelVideoPipelineService = reelVideoPipelineService;
        this.reelCapabilitiesService = reelCapabilitiesService;
        this.reelJobProgressTracker = reelJobProgressTracker;
        this.reelJobProgressBroker = reelJobProgressBroker;
        this.enforceReadSafety = enforceReadSafety;
        this.requireReadyStateForServing = requireReadyStateForServing;
        this.moderationAdminToken = moderationAdminToken;
    }

    /**
     * Direct file upload to local storage.
     * POST /v1/media/upload (multipart/form-data)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DirectUploadResponse uploadFile(
            @RequestParam("owner_id") UUID ownerId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "filename", required = false) String filename,
            @RequestParam(value = "media_type", defaultValue = "image") String mediaType) {
        String name = filename != null ? filename : (file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        log.info(
                "flow=media_upload POST /v1/media/upload ownerId={} partFilename={} declaredFilename={} "
                        + "contentType={} sizeBytes={} mediaTypeParam={}",
                ownerId,
                file.getOriginalFilename(),
                name,
                file.getContentType(),
                file.getSize(),
                mediaType);
        return mediaUploadService.uploadFile(file, ownerId, name, file.getContentType(), mediaType);
    }

    @PostMapping(value = "/wallet/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public WalletDocumentDto uploadWalletDocument(
            @RequestParam("owner_id") UUID ownerId,
            @RequestParam("category") String category,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "filename", required = false) String filename) {
        String name = filename != null ? filename : (file.getOriginalFilename() != null ? file.getOriginalFilename() : "document");
        DirectUploadResponse uploaded = mediaUploadService.uploadFile(file, ownerId, name, file.getContentType(), "other");
        MediaFile tagged = mediaUploadService.tagWalletDocument(uploaded.mediaId(), ownerId, category);
        return toWalletDocumentDto(tagged);
    }

    @GetMapping("/wallet/documents")
    @ResponseStatus(HttpStatus.OK)
    public List<WalletDocumentDto> listWalletDocuments(@RequestParam("owner_id") UUID ownerId) {
        return mediaUploadService.listWalletDocuments(ownerId).stream()
                .map(this::toWalletDocumentDto)
                .toList();
    }

    @DeleteMapping("/wallet/documents/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWalletDocument(
            @PathVariable("mediaId") UUID mediaId,
            @RequestParam("owner_id") UUID ownerId) {
        mediaUploadService.deleteWalletDocument(mediaId, ownerId);
    }

    /**
     * Reel delivery pipeline: smart trim, FFmpeg filters, 9:16 720×1280, optional library music, fast H.264.
     * POST /v1/media/reel/process-upload
     */
    @PostMapping(value = "/reel/process-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ReelProcessResponse processReelUpload(
            @RequestParam("owner_id") UUID ownerId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "filter_type", defaultValue = "NONE") String filterType,
            @RequestParam(value = "music_enabled", defaultValue = "true") boolean musicEnabled,
            @RequestParam(value = "client_job_id", required = false) String clientJobId) throws IOException {
        log.info("POST /v1/media/reel/process-upload ownerId={} filter={} music={} size={} jobId={}",
                ownerId, filterType, musicEnabled, file.getSize(), clientJobId);
        return reelVideoPipelineService.processAndRegister(
                file, ownerId, ReelFilterType.fromParam(filterType), musicEnabled, clientJobId);
    }

    /**
     * Returns reel pipeline capabilities for clients/admins.
     */
    @GetMapping("/reel/capabilities")
    @ResponseStatus(HttpStatus.OK)
    public ReelCapabilitiesResponse reelCapabilities() {
        return reelCapabilitiesService.getCapabilities();
    }

    /**
     * Poll current reel pipeline stage for a job previously started with
     * {@code client_job_id}. Returns {@code stage=UNKNOWN} if the job id is not (yet)
     * tracked; clients should treat that as "still uploading / queued".
     * <p>
     * GET /v1/media/reel/jobs/{jobId}
     */
    @GetMapping("/reel/jobs/{jobId}")
    @ResponseStatus(HttpStatus.OK)
    public ReelJobProgressResponse reelJobProgress(@PathVariable("jobId") String jobId) {
        ReelJobProgressTracker.Snapshot snap = reelJobProgressTracker.get(jobId);
        if (snap == null) {
            return new ReelJobProgressResponse(jobId, "UNKNOWN", 0, null, java.time.Instant.now());
        }
        ReelJobStage stage = snap.stage();
        int percent = snap.percent() != null ? snap.percent() : percentForStage(stage);
        return new ReelJobProgressResponse(
                jobId,
                stage.name(),
                percent,
                snap.message(),
                snap.updatedAt()
        );
    }

    private static int percentForStage(ReelJobStage stage) {
        if (stage == null) {
            return 0;
        }
        return switch (stage) {
            case QUEUED -> 5;
            case OPTIMIZING -> 25;
            case FILTERING -> 55;
            case MUSIC -> 80;
            case FINALIZING -> 92;
            case READY -> 100;
            case FAILED -> 100;
        };
    }

    /**
     * SSE stream of reel pipeline stage transitions for a job.
     * Sends an initial {@code progress} event with the current snapshot (if any),
     * then pushes a new event for every {@link ReelJobProgressBroker#publish}.
     * Stream closes cleanly once the stage reaches {@code READY} or {@code FAILED},
     * or after a 3-minute idle timeout.
     * <p>
     * GET /v1/media/reel/jobs/{jobId}/stream  (Accept: text/event-stream)
     */
    @GetMapping(value = "/reel/jobs/{jobId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter reelJobProgressStream(
            @PathVariable("jobId") String jobId) {
        long timeoutMs = 3 * 60 * 1000L;
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter =
                new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(timeoutMs);

        java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicReference<ReelJobProgressBroker.Subscription> subscriptionRef =
                new java.util.concurrent.atomic.AtomicReference<>();

        Runnable cleanup = () -> {
            if (closed.compareAndSet(false, true)) {
                ReelJobProgressBroker.Subscription s = subscriptionRef.getAndSet(null);
                if (s != null) {
                    try { s.close(); } catch (Exception ignored) {}
                }
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> { try { emitter.complete(); } catch (Exception ignored) {} cleanup.run(); });
        emitter.onError(e -> cleanup.run());

        java.util.function.Consumer<ReelJobProgressTracker.Snapshot> publisher = snap -> {
            if (closed.get() || snap == null) return;
            try {
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                        .name("progress")
                        .data(toStreamResponse(jobId, snap)));
                if (snap.stage() == ReelJobStage.READY || snap.stage() == ReelJobStage.FAILED) {
                    emitter.complete();
                    cleanup.run();
                }
            } catch (Exception e) {
                cleanup.run();
            }
        };

        subscriptionRef.set(reelJobProgressBroker.subscribe(jobId, publisher));

        ReelJobProgressTracker.Snapshot current = reelJobProgressTracker.get(jobId);
        if (current != null) {
            publisher.accept(current);
        } else {
            try {
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                        .name("progress")
                        .data(new ReelJobProgressResponse(jobId, "UNKNOWN", 0, null, java.time.Instant.now())));
            } catch (Exception e) {
                cleanup.run();
            }
        }

        return emitter;
    }

    private static ReelJobProgressResponse toStreamResponse(String jobId, ReelJobProgressTracker.Snapshot snap) {
        ReelJobStage stage = snap.stage();
        int percent = snap.percent() != null ? snap.percent() : percentForStage(stage);
        return new ReelJobProgressResponse(
                jobId,
                stage == null ? "UNKNOWN" : stage.name(),
                percent,
                snap.message(),
                snap.updatedAt() == null ? java.time.Instant.now() : snap.updatedAt()
        );
    }

    /**
     * Serve media file by ID.
     * GET /v1/media/files/{mediaId}
     */
    @GetMapping("/files/{mediaId}")
    public ResponseEntity<InputStreamResource> serveFile(
            @PathVariable("mediaId") UUID mediaId,
            @RequestParam(value = "variant", required = false) String variant) {
        log.debug("flow=media_download GET /v1/media/files/{} variant={}", mediaId, variant);
        MediaFile media = mediaUploadService.getMedia(mediaId);
        assertReadableForStreaming(media);
        com.travelo.mediaservice.service.MediaContentResolutionService.ResolvedContent resolved =
                contentResolutionService.resolve(media, variant);
        String storageKey = resolved.storageKey();
        if (storageKey == null) {
            log.warn("flow=media_download NOT_FOUND mediaId={} reason=null_storage_key variant={}", mediaId, variant);
            return ResponseEntity.notFound().build();
        }
        if (!localStorageService.exists(storageKey)) {
            log.warn("flow=media_download NOT_FOUND mediaId={} storageKey={} (blob missing in storage backend)",
                    mediaId, storageKey);
            return ResponseEntity.notFound().build();
        }
        try {
            InputStream in = localStorageService.getInputStream(storageKey);
            String contentType = resolved.contentType() != null ? resolved.contentType() : "application/octet-stream";
            String fname = resolved.downloadFilename() != null ? resolved.downloadFilename() : "media";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fname.replace("\"", "_") + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                    .body(new InputStreamResource(in));
        } catch (Exception e) {
            log.error("Error serving file for mediaId={}", mediaId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private void assertReadableForStreaming(MediaFile media) {
        if (media == null) {
            return;
        }
        String safety = media.getSafetyStatus() == null ? "unknown" : media.getSafetyStatus().trim().toLowerCase(Locale.ROOT);
        if (enforceReadSafety && ("unsafe".equals(safety) || "review".equals(safety))) {
            throw new SecurityException("Media blocked by moderation policy");
        }
        if (requireReadyStateForServing && media.getState() != com.travelo.mediaservice.entity.MediaStatus.READY) {
            throw new IllegalStateException("Media is not ready for serving");
        }
    }

    /**
     * Request presigned upload URL (deprecated - use /upload for direct upload).
     */
    @Deprecated
    @PostMapping("/upload-url")
    @ResponseStatus(HttpStatus.OK)
    public UploadUrlResponse createUploadUrl(@Valid @RequestBody UploadUrlRequest request) {
        return mediaUploadService.createUploadUrl(request);
    }

    /**
     * Get presigned URLs for multipart upload parts.
     * GET /v1/media/{mediaId}/multipart-urls?parts=10
     */
    @GetMapping("/{mediaId}/multipart-urls")
    @ResponseStatus(HttpStatus.OK)
    public MultipartPartUrlResponse getMultipartPartUrls(
            @PathVariable("mediaId") UUID mediaId,
            @RequestParam int parts) {
        log.info("GET /v1/media/{}/multipart-urls - parts={}", mediaId, parts);
        return mediaUploadService.generateMultipartPartUrls(mediaId, parts);
    }

    /**
     * Confirm upload completion (optional, if client needs to explicitly notify).
     * POST /v1/media/{mediaId}/complete
     */
    @PostMapping("/{mediaId}/complete")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void completeUpload(
            @PathVariable("mediaId") UUID mediaId,
            @Valid @RequestBody ConfirmUploadRequest request) {
        log.info("POST /v1/media/{}/complete - etag={}, sizeBytes={}", mediaId, request.etag(), request.sizeBytes());
        mediaUploadService.completeUpload(mediaId, request);
    }

    /**
     * Get variants for a media file.
     * GET /v1/media/{mediaId}/variants?includeSignedUrls=true
     */
    @GetMapping("/{mediaId}/variants")
    @ResponseStatus(HttpStatus.OK)
    public VariantsResponse getVariants(
            @PathVariable("mediaId") UUID mediaId,
            @RequestParam(name = "includeSignedUrls", defaultValue = "false") boolean includeSignedUrls) {
        log.info("GET /v1/media/{}/variants - includeSignedUrls={}", mediaId, includeSignedUrls);
        VariantsResponse response = mediaUploadService.getVariants(mediaId, includeSignedUrls);
        log.info("GET /v1/media/{}/variants - Success: found {} variants", mediaId,
                response.variants() != null ? response.variants().size() : 0);
        return response;
    }

    /**
     * Generate signed download URL.
     * GET /v1/media/{mediaId}/download?variant=thumb-320&expires=60
     */
    @GetMapping("/{mediaId}/download")
    @ResponseStatus(HttpStatus.OK)
    public DownloadUrlResponse getDownloadUrl(
            @PathVariable("mediaId") UUID mediaId,
            @RequestParam(name = "variant", required = false) String variant,
            @RequestParam(name = "expires", required = false) Integer expires) {
        log.info("GET /v1/media/{}/download - variant={}, expires={}s", mediaId, variant, expires);
        DownloadUrlResponse response = mediaUploadService.generateDownloadUrl(mediaId, variant, expires);
        log.info("GET /v1/media/{}/download - Success: URL generated", mediaId);
        return response;
    }

    /**
     * Get media metadata by ID.
     * GET /v1/media/{mediaId}
     */
    @GetMapping("/{mediaId}")
    @ResponseStatus(HttpStatus.OK)
    public MediaFile getMedia(@PathVariable("mediaId") UUID mediaId) {
        log.debug("GET /v1/media/{}", mediaId);
        return mediaUploadService.getMedia(mediaId);
    }

    /**
     * Admin: Reprocess a media file.
     * POST /v1/media/{mediaId}/reprocess
     */
    @PostMapping("/{mediaId}/reprocess")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void reprocess(
            @PathVariable("mediaId") UUID mediaId,
            @RequestBody(required = false) List<String> processingSteps) {
        log.info("POST /v1/media/{}/reprocess - steps={}", mediaId, processingSteps);
        mediaUploadService.reprocess(mediaId, processingSteps != null ? processingSteps : List.of());
    }

    /**
     * Trim a video file.
     * POST /v1/media/{mediaId}/trim
     */
    @PostMapping("/{mediaId}/trim")
    @ResponseStatus(HttpStatus.OK)
    public com.travelo.mediaservice.dto.ProcessedMediaResponse trimVideo(
            @PathVariable("mediaId") UUID mediaId,
            @Valid @RequestBody com.travelo.mediaservice.dto.TrimVideoRequest request) {
        log.info("POST /v1/media/{}/trim - start={}s, end={}s", mediaId, request.startTimeSeconds(), request.endTimeSeconds());
        return mediaProcessingService.trimVideo(mediaId, request);
    }

    /**
     * Crop an image file.
     * POST /v1/media/{mediaId}/crop
     */
    @PostMapping("/{mediaId}/crop")
    @ResponseStatus(HttpStatus.OK)
    public com.travelo.mediaservice.dto.ProcessedMediaResponse cropImage(
            @PathVariable("mediaId") UUID mediaId,
            @Valid @RequestBody com.travelo.mediaservice.dto.CropImageRequest request) {
        log.info("POST /v1/media/{}/crop - x={}, y={}, width={}, height={}", 
                mediaId, request.x(), request.y(), request.width(), request.height());
        return mediaProcessingService.cropImage(mediaId, request);
    }

    /**
     * Rotate an image file.
     * POST /v1/media/{mediaId}/rotate
     */
    @PostMapping("/{mediaId}/rotate")
    @ResponseStatus(HttpStatus.OK)
    public com.travelo.mediaservice.dto.ProcessedMediaResponse rotateImage(
            @PathVariable("mediaId") UUID mediaId,
            @Valid @RequestBody com.travelo.mediaservice.dto.RotateImageRequest request) {
        log.info("POST /v1/media/{}/rotate - angle={}°", mediaId, request.angleDegrees());
        return mediaProcessingService.rotateImage(mediaId, request);
    }

    /**
     * Admin moderation queue.
     * GET /v1/media/admin/moderation/queue?limit=100
     */
    @GetMapping("/admin/moderation/queue")
    @ResponseStatus(HttpStatus.OK)
    public List<ModerationQueueItemResponse> getModerationQueue(
            @RequestHeader(value = "X-Moderation-Admin-Token", required = false) String adminToken,
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        requireModerationAdmin(adminToken);
        return mediaUploadService.getModerationQueue(limit).stream()
                .map(media -> new ModerationQueueItemResponse(
                        media.getId(),
                        media.getOwnerId(),
                        media.getFilename(),
                        media.getMimeType(),
                        media.getState() != null ? media.getState().name().toLowerCase() : "unknown",
                        media.getSafetyStatus(),
                        media.getStorageKey(),
                        media.getMeta(),
                        media.getCreatedAt(),
                        media.getUpdatedAt()
                ))
                .toList();
    }

    /**
     * Admin moderation decision.
     * POST /v1/media/admin/moderation/{mediaId}/decision
     */
    @PostMapping("/admin/moderation/{mediaId}/decision")
    @ResponseStatus(HttpStatus.OK)
    public ModerationQueueItemResponse applyModerationDecision(
            @RequestHeader(value = "X-Moderation-Admin-Token", required = false) String adminToken,
            @RequestHeader(value = "X-Moderation-Reviewer", required = false) String reviewer,
            @PathVariable("mediaId") UUID mediaId,
            @Valid @RequestBody ModerationDecisionRequest request) {
        requireModerationAdmin(adminToken);
        MediaFile media = mediaUploadService.applyModerationDecision(
                mediaId,
                request.getDecision(),
                request.getReason(),
                reviewer
        );
        return new ModerationQueueItemResponse(
                media.getId(),
                media.getOwnerId(),
                media.getFilename(),
                media.getMimeType(),
                media.getState() != null ? media.getState().name().toLowerCase() : "unknown",
                media.getSafetyStatus(),
                media.getStorageKey(),
                media.getMeta(),
                media.getCreatedAt(),
                media.getUpdatedAt()
        );
    }

    private void requireModerationAdmin(String adminToken) {
        if (moderationAdminToken == null || moderationAdminToken.isBlank()) {
            throw new IllegalStateException("Moderation admin token is not configured");
        }
        if (adminToken == null || !moderationAdminToken.equals(adminToken)) {
            throw new SecurityException("Invalid moderation admin token");
        }
    }

    private WalletDocumentDto toWalletDocumentDto(MediaFile media) {
        String category = "other";
        if (media.getMeta() != null && media.getMeta().get("wallet_category") != null) {
            category = String.valueOf(media.getMeta().get("wallet_category"));
        }
        String downloadUrl = "/v1/media/files/" + media.getId();
        return new WalletDocumentDto(
                media.getId(),
                media.getOwnerId(),
                category,
                media.getFilename(),
                media.getMimeType(),
                media.getSizeBytes(),
                downloadUrl,
                media.getCreatedAt()
        );
    }
}
