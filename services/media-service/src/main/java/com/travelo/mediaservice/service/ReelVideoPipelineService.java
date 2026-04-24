package com.travelo.mediaservice.service;

import com.travelo.mediaservice.dto.DirectUploadResponse;
import com.travelo.mediaservice.dto.ReelProcessResponse;
import com.travelo.mediaservice.entity.MediaFile;
import com.travelo.mediaservice.entity.MediaVariant;
import com.travelo.mediaservice.reel.ReelFilterType;
import com.travelo.mediaservice.reel.ReelJobProgressTracker;
import com.travelo.mediaservice.reel.ReelJobStage;
import com.travelo.mediaservice.reel.ReelProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates FFmpeg reel delivery + storage; falls back to a plain upload on failure.
 */
@Service
public class ReelVideoPipelineService {

    private static final Logger log = LoggerFactory.getLogger(ReelVideoPipelineService.class);
    private static final String FFPROBE = "ffprobe";

    private final ReelProcessingService reelProcessingService;
    private final MediaUploadService mediaUploadService;
    private final MediaDownloadUrlBuilder downloadUrlBuilder;
    private final ReelJobProgressTracker progressTracker;

    public ReelVideoPipelineService(ReelProcessingService reelProcessingService,
                                   MediaUploadService mediaUploadService,
                                   MediaDownloadUrlBuilder downloadUrlBuilder,
                                   ReelJobProgressTracker progressTracker) {
        this.reelProcessingService = reelProcessingService;
        this.mediaUploadService = mediaUploadService;
        this.downloadUrlBuilder = downloadUrlBuilder;
        this.progressTracker = progressTracker;
    }

    public ReelProcessResponse processAndRegister(MultipartFile file,
                                                UUID ownerId,
                                                ReelFilterType filterType,
                                                boolean musicEnabled,
                                                String clientJobId) throws IOException {
        String progressKey = clientJobId != null && !clientJobId.isBlank() ? clientJobId : null;
        if (progressKey != null) {
            progressTracker.report(progressKey, ReelJobStage.QUEUED, "Queued");
        }
        byte[] raw = file.getBytes();
        File tempIn = Files.createTempFile("reel_src_", ".mp4").toFile();
        try {
            Files.write(tempIn.toPath(), raw);
            UUID jobId = UUID.randomUUID();
            File processed = reelProcessingService.processReelDelivery(
                    tempIn, jobId, filterType, musicEnabled, progressKey);
            int durationSec = (int) Math.round(probeDurationSeconds(processed));
            if (progressKey != null) {
                progressTracker.report(progressKey, ReelJobStage.FINALIZING, "Finalizing");
            }
            DirectUploadResponse up = mediaUploadService.uploadReelProcessedDelivery(
                    ownerId, processed, safeName(file.getOriginalFilename()));
            try {
                Files.deleteIfExists(processed.toPath());
            } catch (Exception ignored) {
            }
            ReelProcessResponse response = buildResponse(up, durationSec, false);
            if (progressKey != null) {
                progressTracker.report(progressKey, ReelJobStage.READY, "Ready");
            }
            return response;
        } catch (Exception e) {
            log.error("Reel FFmpeg pipeline failed; falling back to raw upload: {}", e.toString());
            if (progressKey != null) {
                progressTracker.report(progressKey, ReelJobStage.FINALIZING,
                        "Finalizing (fallback upload)");
            }
            String ct = file.getContentType() != null ? file.getContentType() : "video/mp4";
            DirectUploadResponse up = mediaUploadService.uploadRawBytes(
                    ownerId, raw, safeName(file.getOriginalFilename()), ct, "video");
            ReelProcessResponse response = buildResponse(up, null, true);
            if (progressKey != null) {
                progressTracker.report(progressKey, ReelJobStage.READY, "Ready (fallback)");
            }
            return response;
        } finally {
            try {
                Files.deleteIfExists(tempIn.toPath());
            } catch (Exception ignored) {
            }
        }
    }

    private ReelProcessResponse buildResponse(DirectUploadResponse up, Integer durationSeconds, boolean fallback) {
        String thumb = null;
        try {
            MediaFile mf = mediaUploadService.getMedia(up.mediaId());
            List<MediaVariant> variants = mf.getVariants();
            if (variants != null) {
                for (MediaVariant v : variants) {
                    if (v != null && "thumbnail".equals(v.getName()) && v.getKey() != null) {
                        thumb = downloadUrlBuilder.buildVariantUrl(up.mediaId(), "thumbnail", v.getKey());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not resolve thumbnail URL mediaId={}", up.mediaId());
        }
        return new ReelProcessResponse(up.mediaId(), up.downloadUrl(), up.storageKey(), durationSeconds, thumb, fallback);
    }

    private static String safeName(String original) {
        if (original == null || original.isBlank()) {
            return "reel_upload.mp4";
        }
        return original;
    }

    private static double probeDurationSeconds(File f) throws IOException, InterruptedException {
        List<String> cmd = List.of(
                FFPROBE, "-v", "error", "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1", f.getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            out = r.readLine();
        }
        int code = p.waitFor();
        if (code != 0 || out == null) {
            return 0;
        }
        try {
            return Double.parseDouble(out.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
