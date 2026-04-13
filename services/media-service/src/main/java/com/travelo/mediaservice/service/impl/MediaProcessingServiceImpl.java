package com.travelo.mediaservice.service.impl;

import com.travelo.mediaservice.dto.CropImageRequest;
import com.travelo.mediaservice.dto.ProcessedMediaResponse;
import com.travelo.mediaservice.dto.RotateImageRequest;
import com.travelo.mediaservice.dto.TrimVideoRequest;
import com.travelo.mediaservice.entity.MediaFile;
import com.travelo.mediaservice.entity.MediaStatus;
import com.travelo.mediaservice.entity.MediaType;
import com.travelo.mediaservice.entity.MediaVariant;
import com.travelo.mediaservice.exception.MediaFileNotFoundException;
import com.travelo.mediaservice.repository.MediaFileRepository;
import com.travelo.mediaservice.service.LocalStorageService;
import com.travelo.mediaservice.service.MediaDownloadUrlBuilder;
import com.travelo.mediaservice.service.MediaModerationService;
import com.travelo.mediaservice.service.MediaProcessingService;
import com.travelo.mediaservice.service.ThumbnailService;
import com.travelo.mediaservice.service.VirusScanService;
import com.travelo.mediaservice.util.LocalStorageKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MediaProcessingServiceImpl implements MediaProcessingService {

    private static final Logger log = LoggerFactory.getLogger(MediaProcessingServiceImpl.class);
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    private static final String FFMPEG_COMMAND = "ffmpeg";
    private final MediaFileRepository mediaFileRepository;
    private final VirusScanService virusScanService;
    private final MediaModerationService moderationService;
    private final ThumbnailService thumbnailService;
    private final LocalStorageService localStorageService;
    private final MediaDownloadUrlBuilder downloadUrlBuilder;

    public MediaProcessingServiceImpl(MediaFileRepository mediaFileRepository,
                                     VirusScanService virusScanService,
                                     MediaModerationService moderationService,
                                     ThumbnailService thumbnailService,
                                     LocalStorageService localStorageService,
                                     MediaDownloadUrlBuilder downloadUrlBuilder) {
        this.mediaFileRepository = mediaFileRepository;
        this.virusScanService = virusScanService;
        this.moderationService = moderationService;
        this.thumbnailService = thumbnailService;
        this.localStorageService = localStorageService;
        this.downloadUrlBuilder = downloadUrlBuilder;
    }

    @Override
    @Transactional
    public void processMedia(UUID mediaId) {
        processMedia(mediaId, List.of("virus_scan", "moderation", "transcode", "thumbnail"));
    }

    @Override
    @Transactional
    public void processMedia(UUID mediaId, List<String> processingSteps) {
        log.info("Processing mediaId={} with steps={}", mediaId, processingSteps);

        MediaFile media = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new MediaFileNotFoundException(mediaId));

        try {
            media.setState(MediaStatus.PROCESSING);
            mediaFileRepository.save(media);

            if (shouldProcessStep(processingSteps, "virus_scan")) {
                boolean isClean = virusScanService.scanFile(media.getStorageBucket(), media.getStorageKey());
                if (!isClean) {
                    log.warn("Virus detected in mediaId={}", mediaId);
                    media.setState(MediaStatus.INFECTED);
                    media.setSafetyStatus("infected");
                    media.getMeta().put("virus_scan", Map.of("result", "infected", "threat", "unknown"));
                    mediaFileRepository.save(media);
                    return;
                }
                media.getMeta().put("virus_scan", Map.of("result", "clean", "timestamp", java.time.Instant.now()));
            }

            if (shouldProcessStep(processingSteps, "moderation")) {
                moderationService.moderateMedia(mediaId);
                media = mediaFileRepository.findById(mediaId).orElseThrow(() -> new MediaFileNotFoundException(mediaId));
                if (media.getState().equals(MediaStatus.UNSAFE) || media.getState().equals(MediaStatus.REVIEW)) {
                    return;
                }
            }

            if (shouldProcessStep(processingSteps, "transcode") &&
                (media.getMediaType() == MediaType.VIDEO || media.getMediaType() == MediaType.AUDIO)) {
                // TODO: transcoding
            }

            if (shouldProcessStep(processingSteps, "thumbnail") && media.getMediaType() == MediaType.VIDEO) {
                try {
                    String thumbnailKey = thumbnailService.generateThumbnailFromLocal(
                            media.getStorageKey(), media.getId());
                    if (thumbnailKey != null) {
                        MediaVariant thumbnailVariant = new MediaVariant("thumbnail", thumbnailKey, "image/jpeg", 640, 360);
                        List<MediaVariant> variants = new ArrayList<>(media.getVariants());
                        variants.add(thumbnailVariant);
                        media.setVariants(variants);
                        media.getMeta().put("thumbnail_key", thumbnailKey);
                    }
                } catch (Exception e) {
                    log.error("Error generating thumbnail for mediaId={}", mediaId, e);
                    media.getMeta().put("thumbnail_error", e.getMessage());
                }
            }

            if (!media.getState().equals(MediaStatus.UNSAFE) && !media.getState().equals(MediaStatus.REVIEW) && !media.getState().equals(MediaStatus.INFECTED)) {
                media.setState(MediaStatus.READY);
            }
            mediaFileRepository.save(media);
        } catch (Exception e) {
            log.error("Error processing mediaId={}", mediaId, e);
            media.setState(MediaStatus.UPLOAD_PENDING);
            media.getMeta().put("processing_error", e.getMessage());
            mediaFileRepository.save(media);
            throw e;
        }
    }

    private boolean shouldProcessStep(List<String> steps, String stepName) {
        return steps == null || steps.isEmpty() || steps.contains(stepName);
    }

    @Override
    @Transactional
    public ProcessedMediaResponse trimVideo(UUID mediaId, TrimVideoRequest request) {
        MediaFile originalMedia = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new MediaFileNotFoundException(mediaId));
        if (originalMedia.getMediaType() != MediaType.VIDEO) {
            throw new IllegalArgumentException("Media is not a video: " + mediaId);
        }

        File tempVideoFile = null;
        File trimmedVideoFile = null;
        try {
            tempVideoFile = getFileFromStorage(originalMedia.getStorageKey(), mediaId, "video");
            if (tempVideoFile == null) throw new RuntimeException("Failed to read video");

            trimmedVideoFile = trimVideoWithFFmpeg(tempVideoFile, mediaId, request.startTimeSeconds(), request.endTimeSeconds());
            if (trimmedVideoFile == null) throw new RuntimeException("Failed to trim video");

            UUID processedMediaId = UUID.randomUUID();
            String processedKey = LocalStorageKeyGenerator.rawOriginalKey(processedMediaId);
            saveToStorage(processedKey, trimmedVideoFile, originalMedia.getMimeType());

            MediaFile processedMedia = new MediaFile(originalMedia.getOwnerId(), MediaType.VIDEO, originalMedia.getMimeType(),
                    "trimmed_" + originalMedia.getFilename(), trimmedVideoFile.length(), processedKey, originalMedia.getStorageBucket());
            processedMedia.setId(processedMediaId);
            processedMedia.setState(MediaStatus.READY);
            processedMedia.getMeta().put("original_media_id", mediaId.toString());
            processedMedia = mediaFileRepository.save(processedMedia);

            String downloadUrl = downloadUrlBuilder.buildUploadDownloadUrl(processedMediaId, processedKey);
            return new ProcessedMediaResponse(processedMediaId, processedKey, downloadUrl, "Video trimmed successfully");
        } finally {
            cleanupTempFile(tempVideoFile);
            cleanupTempFile(trimmedVideoFile);
        }
    }

    @Override
    @Transactional
    public ProcessedMediaResponse cropImage(UUID mediaId, CropImageRequest request) {
        MediaFile originalMedia = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new MediaFileNotFoundException(mediaId));
        if (originalMedia.getMediaType() != MediaType.IMAGE) {
            throw new IllegalArgumentException("Media is not an image: " + mediaId);
        }

        File tempImageFile = null;
        File croppedImageFile = null;
        try {
            tempImageFile = getFileFromStorage(originalMedia.getStorageKey(), mediaId, "image");
            if (tempImageFile == null) throw new RuntimeException("Failed to read image");

            croppedImageFile = cropImageWithJava(tempImageFile, mediaId, request.x(), request.y(), request.width(), request.height());
            if (croppedImageFile == null) throw new RuntimeException("Failed to crop image");

            UUID processedMediaId = UUID.randomUUID();
            String processedKey = LocalStorageKeyGenerator.rawOriginalKey(processedMediaId);
            saveToStorage(processedKey, croppedImageFile, originalMedia.getMimeType());

            MediaFile processedMedia = new MediaFile(originalMedia.getOwnerId(), MediaType.IMAGE, originalMedia.getMimeType(),
                    "cropped_" + originalMedia.getFilename(), croppedImageFile.length(), processedKey, originalMedia.getStorageBucket());
            processedMedia.setId(processedMediaId);
            processedMedia.setState(MediaStatus.READY);
            processedMedia.getMeta().put("original_media_id", mediaId.toString());
            processedMedia = mediaFileRepository.save(processedMedia);

            String downloadUrl = downloadUrlBuilder.buildUploadDownloadUrl(processedMediaId, processedKey);
            return new ProcessedMediaResponse(processedMediaId, processedKey, downloadUrl, "Image cropped successfully");
        } finally {
            cleanupTempFile(tempImageFile);
            cleanupTempFile(croppedImageFile);
        }
    }

    @Override
    @Transactional
    public ProcessedMediaResponse rotateImage(UUID mediaId, RotateImageRequest request) {
        MediaFile originalMedia = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new MediaFileNotFoundException(mediaId));
        if (originalMedia.getMediaType() != MediaType.IMAGE) {
            throw new IllegalArgumentException("Media is not an image: " + mediaId);
        }

        File tempImageFile = null;
        File rotatedImageFile = null;
        try {
            tempImageFile = getFileFromStorage(originalMedia.getStorageKey(), mediaId, "image");
            if (tempImageFile == null) throw new RuntimeException("Failed to read image");

            rotatedImageFile = rotateImageWithJava(tempImageFile, mediaId, request.angleDegrees());
            if (rotatedImageFile == null) throw new RuntimeException("Failed to rotate image");

            UUID processedMediaId = UUID.randomUUID();
            String processedKey = LocalStorageKeyGenerator.rawOriginalKey(processedMediaId);
            saveToStorage(processedKey, rotatedImageFile, originalMedia.getMimeType());

            MediaFile processedMedia = new MediaFile(originalMedia.getOwnerId(), MediaType.IMAGE, originalMedia.getMimeType(),
                    "rotated_" + originalMedia.getFilename(), rotatedImageFile.length(), processedKey, originalMedia.getStorageBucket());
            processedMedia.setId(processedMediaId);
            processedMedia.setState(MediaStatus.READY);
            processedMedia.getMeta().put("original_media_id", mediaId.toString());
            processedMedia = mediaFileRepository.save(processedMedia);

            String downloadUrl = downloadUrlBuilder.buildUploadDownloadUrl(processedMediaId, processedKey);
            return new ProcessedMediaResponse(processedMediaId, processedKey, downloadUrl, "Image rotated successfully");
        } finally {
            cleanupTempFile(tempImageFile);
            cleanupTempFile(rotatedImageFile);
        }
    }

    private File getFileFromStorage(String storageKey, UUID mediaId, String type) {
        File file = localStorageService.getFile(storageKey);
        if (file != null && file.exists()) return file;
        return null;
    }

    private void saveToStorage(String storageKey, File file, String contentType) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            localStorageService.save(storageKey, bytes, contentType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save to storage", e);
        }
    }

    private File trimVideoWithFFmpeg(File inputVideo, UUID mediaId, Double startTime, Double endTime) {
        try {
            Path tempDir = Paths.get(TEMP_DIR, "processed");
            Files.createDirectories(tempDir);
            String ext = inputVideo.getName().contains(".") ? inputVideo.getName().substring(inputVideo.getName().lastIndexOf('.')) : ".mp4";
            File outputFile = tempDir.resolve("trimmed_" + mediaId + ext).toFile();
            double duration = endTime - startTime;

            ProcessBuilder pb = new ProcessBuilder(FFMPEG_COMMAND, "-i", inputVideo.getAbsolutePath(),
                    "-ss", String.valueOf(startTime), "-t", String.valueOf(duration), "-c", "copy", "-y", outputFile.getAbsolutePath());
            pb.redirectErrorStream(true);
            int exitCode = pb.start().waitFor();
            return exitCode == 0 && outputFile.exists() ? outputFile : null;
        } catch (Exception e) {
            log.error("Error trimming video: mediaId={}", mediaId, e);
            return null;
        }
    }

    private File cropImageWithJava(File inputImage, UUID mediaId, int x, int y, int width, int height) {
        try {
            BufferedImage originalImage = ImageIO.read(inputImage);
            if (originalImage == null) return null;
            BufferedImage croppedImage = originalImage.getSubimage(x, y, width, height);
            Path tempDir = Paths.get(TEMP_DIR, "processed");
            Files.createDirectories(tempDir);
            String ext = inputImage.getName().contains(".") ? inputImage.getName().substring(inputImage.getName().lastIndexOf('.')) : ".jpg";
            File outputFile = tempDir.resolve("cropped_" + mediaId + ext).toFile();
            String format = ext.substring(1).toLowerCase();
            if (format.equals("jpg")) format = "jpeg";
            ImageIO.write(croppedImage, format, outputFile);
            return outputFile;
        } catch (Exception e) {
            log.error("Error cropping image: mediaId={}", mediaId, e);
            return null;
        }
    }

    private File rotateImageWithJava(File inputImage, UUID mediaId, Double angleDegrees) {
        try {
            BufferedImage originalImage = ImageIO.read(inputImage);
            if (originalImage == null) return null;
            double radians = Math.toRadians(angleDegrees);
            double sin = Math.abs(Math.sin(radians));
            double cos = Math.abs(Math.cos(radians));
            int newWidth = (int) Math.round(originalImage.getWidth() * cos + originalImage.getHeight() * sin);
            int newHeight = (int) Math.round(originalImage.getWidth() * sin + originalImage.getHeight() * cos);
            BufferedImage rotatedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
            Graphics2D g2d = rotatedImage.createGraphics();
            AffineTransform transform = AffineTransform.getRotateInstance(radians, originalImage.getWidth() / 2.0, originalImage.getHeight() / 2.0);
            g2d.drawImage(originalImage, transform, null);
            g2d.dispose();
            Path tempDir = Paths.get(TEMP_DIR, "processed");
            Files.createDirectories(tempDir);
            String ext = inputImage.getName().contains(".") ? inputImage.getName().substring(inputImage.getName().lastIndexOf('.')) : ".jpg";
            File outputFile = tempDir.resolve("rotated_" + mediaId + ext).toFile();
            String format = ext.substring(1).toLowerCase();
            if (format.equals("jpg")) format = "jpeg";
            ImageIO.write(rotatedImage, format, outputFile);
            return outputFile;
        } catch (Exception e) {
            log.error("Error rotating image: mediaId={}", mediaId, e);
            return null;
        }
    }

    private void cleanupTempFile(File file) {
        if (file != null && file.exists()) {
            try { file.delete(); } catch (Exception e) { log.warn("Failed to delete temp file: {}", file); }
        }
    }
}
