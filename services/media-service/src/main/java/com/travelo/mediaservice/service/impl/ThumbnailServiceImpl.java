package com.travelo.mediaservice.service.impl;

import com.travelo.mediaservice.service.LocalStorageService;
import com.travelo.mediaservice.service.ThumbnailService;
import com.travelo.mediaservice.util.LocalStorageKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class ThumbnailServiceImpl implements ThumbnailService {

    private static final Logger log = LoggerFactory.getLogger(ThumbnailServiceImpl.class);
    private static final String FFMPEG_COMMAND = "ffmpeg";
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    private static final int THUMBNAIL_WIDTH = 640;
    private static final int THUMBNAIL_HEIGHT = 360;
    private static final double THUMBNAIL_TIME_OFFSET = 1.0;

    private final LocalStorageService localStorageService;

    public ThumbnailServiceImpl(LocalStorageService localStorageService) {
        this.localStorageService = localStorageService;
    }

    @Override
    public File generateThumbnail(File videoFile, UUID mediaId) {
        if (videoFile == null || !videoFile.exists()) return null;
        try {
            Path tempDir = Paths.get(TEMP_DIR, "thumbnails");
            Files.createDirectories(tempDir);
            File thumbnailFile = tempDir.resolve("thumbnail_" + mediaId + ".jpg").toFile();

            ProcessBuilder pb = new ProcessBuilder(FFMPEG_COMMAND, "-i", videoFile.getAbsolutePath(),
                    "-ss", String.valueOf(THUMBNAIL_TIME_OFFSET), "-vframes", "1",
                    "-vf", String.format("scale=%d:%d:force_original_aspect_ratio=decrease,pad=%d:%d:(ow-iw)/2:(oh-ih)/2",
                            THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT),
                    "-q:v", "2", "-y", thumbnailFile.getAbsolutePath());
            pb.redirectErrorStream(true);
            int exitCode = pb.start().waitFor();
            return exitCode == 0 && thumbnailFile.exists() ? thumbnailFile : null;
        } catch (Exception e) {
            log.error("Error generating thumbnail: mediaId={}", mediaId, e);
            return null;
        }
    }

    @Override
    public String generateThumbnailFromLocal(String storageKey, UUID mediaId) {
        File videoFile = localStorageService.getFile(storageKey);
        if (videoFile == null || !videoFile.exists()) {
            log.error("Video file not found: {}", storageKey);
            return null;
        }
        File thumbnailFile = generateThumbnail(videoFile, mediaId);
        if (thumbnailFile == null) return null;

        String thumbnailKey = LocalStorageKeyGenerator.processedVariantKey(mediaId, "thumbnail.jpg");
        try {
            byte[] bytes = Files.readAllBytes(thumbnailFile.toPath());
            localStorageService.save(thumbnailKey, bytes, "image/jpeg");
            thumbnailFile.delete();
            return thumbnailKey;
        } catch (IOException e) {
            log.error("Failed to save thumbnail: {}", thumbnailKey, e);
            return null;
        }
    }
}
