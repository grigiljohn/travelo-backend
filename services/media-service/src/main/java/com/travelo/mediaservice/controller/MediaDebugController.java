package com.travelo.mediaservice.controller;

import com.travelo.mediaservice.config.MediaS3Properties;
import com.travelo.mediaservice.dto.DebugUploadTestResponse;
import com.travelo.mediaservice.service.LocalStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Temporary diagnostics: verifies multipart receive + blob write (local disk or S3).
 * Disable with {@code media.debug.upload-test-enabled=false}.
 */
@RestController
@RequestMapping("/api/debug")
@ConditionalOnProperty(prefix = "media.debug", name = "upload-test-enabled", havingValue = "true")
public class MediaDebugController {

    private static final Logger log = LoggerFactory.getLogger(MediaDebugController.class);

    private final LocalStorageService localStorageService;
    private final MediaS3Properties mediaS3Properties;

    public MediaDebugController(LocalStorageService localStorageService,
                               MediaS3Properties mediaS3Properties) {
        this.localStorageService = localStorageService;
        this.mediaS3Properties = mediaS3Properties;
    }

    @PostMapping(value = "/upload-test", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DebugUploadTestResponse uploadTest(@RequestParam("file") MultipartFile file) {
        String safeName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.bin";
        safeName = safeName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safeName.length() > 120) {
            safeName = safeName.substring(0, 120);
        }
        String storageKey = "debug/upload-test/" + UUID.randomUUID() + "/" + safeName;

        log.info(
                "flow=debug_upload_test received originalFilename={} size={} contentType={} s3Enabled={} bucket={}",
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType(),
                mediaS3Properties.isEnabled(),
                mediaS3Properties.isEnabled() ? mediaS3Properties.getBucket() : "(local)");

        try {
            localStorageService.save(storageKey, file);
        } catch (Exception e) {
            log.error("flow=debug_upload_test SAVE_FAILED key={}", storageKey, e);
            return new DebugUploadTestResponse(
                    "ERROR",
                    storageKey,
                    mediaS3Properties.isEnabled(),
                    mediaS3Properties.getBucket(),
                    mediaS3Properties.getRegion(),
                    file.getSize(),
                    file.getContentType(),
                    false,
                    e.getMessage());
        }

        boolean exists = localStorageService.exists(storageKey);
        log.info("flow=debug_upload_test DONE key={} existsAfterWrite={}", storageKey, exists);
        return new DebugUploadTestResponse(
                exists ? "OK" : "FAIL",
                storageKey,
                mediaS3Properties.isEnabled(),
                mediaS3Properties.getBucket(),
                mediaS3Properties.getRegion(),
                file.getSize(),
                file.getContentType(),
                exists,
                exists ? "Write verified" : "Object missing after put");
    }
}
