package com.travelo.mediaservice.dev;

import com.travelo.mediaservice.service.LocalStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;

/**
 * Writes on-disk blobs for Flyway-seeded demo {@code media} rows so GET /v1/media/files/{id} succeeds locally.
 * Keys must stay in sync with {@code V3__seed_demo_media_for_post_service.sql}
 * and {@code V4__seed_story_demo_media.sql}.
 */
@Component
@Order(2000)
public class DemoLocalMediaBlobSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoLocalMediaBlobSeeder.class);

    /** 1×1 JPEG; size must match {@code size_bytes} in V3 seed for images (269). */
    private static final byte[] MINIMAL_JPEG = Base64.getDecoder().decode(
            "/9j/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/"
                    + "2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/"
                    + "wAARCAAyADIDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAf/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/"
                    + "8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCwAA8A/9k=");

    public static final UUID DEMO_IMAGE_MEDIA_1 = UUID.fromString("f1111111-1111-4111-8111-111111111101");
    public static final UUID DEMO_IMAGE_MEDIA_2 = UUID.fromString("f1111111-1111-4111-8111-111111111102");
    public static final UUID DEMO_IMAGE_MEDIA_3 = UUID.fromString("f1111111-1111-4111-8111-111111111103");
    public static final UUID DEMO_VIDEO_MEDIA = UUID.fromString("f1111111-1111-4111-8111-111111111104");
    /** Story demo primary (Cappadocia); matches V4__seed_story_demo_media.sql. */
    public static final UUID DEMO_STORY_IMAGE_MEDIA = UUID.fromString("f1111111-1111-4111-8111-111111111105");

    private final LocalStorageService localStorageService;

    @Value("${media.dev.seed-demo-files:true}")
    private boolean seedDemoFiles;

    public DemoLocalMediaBlobSeeder(LocalStorageService localStorageService) {
        this.localStorageService = localStorageService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seedDemoFiles) {
            return;
        }
        writeIfMissing("demo-seed/" + DEMO_IMAGE_MEDIA_1 + "/original.jpg", MINIMAL_JPEG);
        writeIfMissing("demo-seed/" + DEMO_IMAGE_MEDIA_2 + "/original.jpg", MINIMAL_JPEG);
        writeIfMissing("demo-seed/" + DEMO_IMAGE_MEDIA_3 + "/original.jpg", MINIMAL_JPEG);
        writeIfMissing("demo-seed/" + DEMO_STORY_IMAGE_MEDIA + "/original.jpg", MINIMAL_JPEG);
        String videoKey = "demo-seed/" + DEMO_VIDEO_MEDIA + "/original.mp4";
        if (!localStorageService.exists(videoKey)) {
            ClassPathResource mp4 = new ClassPathResource("seed/demo-video.mp4");
            if (!mp4.exists()) {
                log.warn("Classpath seed/demo-video.mp4 missing; demo video media {} will 404 until file is present", DEMO_VIDEO_MEDIA);
                return;
            }
            try (InputStream in = mp4.getInputStream()) {
                byte[] bytes = in.readAllBytes();
                localStorageService.save(videoKey, bytes, "video/mp4");
                log.info("Wrote demo video seed to storage key {}", videoKey);
            } catch (Exception e) {
                log.error("Failed to write demo video seed to {}", videoKey, e);
            }
        }
    }

    private void writeIfMissing(String relativePath, byte[] bytes) {
        try {
            if (localStorageService.exists(relativePath)) {
                return;
            }
            localStorageService.save(relativePath, bytes, "image/jpeg");
            log.debug("Wrote demo image seed {}", relativePath);
        } catch (Exception e) {
            log.error("Failed to write demo seed {}", relativePath, e);
        }
    }
}
