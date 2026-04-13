package com.travelo.mediaservice.controller;

import com.travelo.mediaservice.dto.*;
import com.travelo.mediaservice.entity.MediaFile;
import com.travelo.mediaservice.service.MediaUploadService;
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

import java.io.InputStream;
import java.util.List;
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

    public MediaController(MediaUploadService mediaUploadService,
                          com.travelo.mediaservice.service.MediaProcessingService mediaProcessingService,
                          com.travelo.mediaservice.service.LocalStorageService localStorageService) {
        this.mediaUploadService = mediaUploadService;
        this.mediaProcessingService = mediaProcessingService;
        this.localStorageService = localStorageService;
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
        String storageKey = variant != null && !variant.isEmpty()
                ? media.getVariants().stream()
                    .filter(v -> v != null && variant.equals(v.getName()))
                    .map(com.travelo.mediaservice.entity.MediaVariant::getKey)
                    .filter(k -> k != null && !k.trim().isEmpty())
                    .findFirst()
                    .orElse(null)
                : media.getStorageKey();
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
            String contentType = media.getMimeType() != null ? media.getMimeType() : "application/octet-stream";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (media.getFilename() != null ? media.getFilename() : "media") + "\"")
                    .body(new InputStreamResource(in));
        } catch (Exception e) {
            log.error("Error serving file for mediaId={}", mediaId, e);
            return ResponseEntity.internalServerError().build();
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
}
