package com.travelo.mediaservice.service.impl;

import com.travelo.mediaservice.entity.MediaFile;
import com.travelo.mediaservice.entity.MediaStatus;
import com.travelo.mediaservice.exception.MediaFileNotFoundException;
import com.travelo.mediaservice.repository.MediaFileRepository;
import com.travelo.mediaservice.service.MediaModerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.ModerationLabel;
import software.amazon.awssdk.services.rekognition.model.S3Object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

/**
 * Content moderation implementation.
 * Provider modes:
 * - mock_keywords: classify by configured keywords in filename/storage key
 * - allow_all: bypass moderation and mark safe
 */
@Service
public class MediaModerationServiceImpl implements MediaModerationService {

    private static final Logger log = LoggerFactory.getLogger(MediaModerationServiceImpl.class);

    private final MediaFileRepository mediaFileRepository;
    private final RekognitionClient rekognitionClient;
    private final String providerMode;
    private final String s3Bucket;
    private final boolean s3Enabled;
    private final List<String> hateKeywords;
    private final List<String> violenceKeywords;
    private final List<String> sexualKeywords;
    private final double unsafeThreshold;
    private final double reviewThreshold;
    private final float awsMinConfidence;
    private final boolean awsFailOpen;

    public MediaModerationServiceImpl(
            MediaFileRepository mediaFileRepository,
            @Autowired(required = false) RekognitionClient rekognitionClient,
            @Value("${media.moderation.provider-mode:mock_keywords}") String providerMode,
            @Value("${media.storage.s3.bucket:}") String s3Bucket,
            @Value("${media.storage.s3.enabled:false}") boolean s3Enabled,
            @Value("${media.moderation.keywords.hate:hate,slur,racist}") String hateKeywordsCsv,
            @Value("${media.moderation.keywords.violence:violence,gore,blood,kill,weapon}") String violenceKeywordsCsv,
            @Value("${media.moderation.keywords.sexual:nsfw,nude,porn,sex,explicit}") String sexualKeywordsCsv,
            @Value("${media.moderation.unsafe-threshold:0.85}") double unsafeThreshold,
            @Value("${media.moderation.review-threshold:0.55}") double reviewThreshold,
            @Value("${media.moderation.aws.min-confidence:60}") float awsMinConfidence,
            @Value("${media.moderation.aws.fail-open:false}") boolean awsFailOpen) {
        this.mediaFileRepository = mediaFileRepository;
        this.rekognitionClient = rekognitionClient;
        this.providerMode = providerMode == null ? "mock_keywords" : providerMode.trim().toLowerCase(Locale.ROOT);
        this.s3Bucket = s3Bucket;
        this.s3Enabled = s3Enabled;
        this.hateKeywords = parseKeywords(hateKeywordsCsv);
        this.violenceKeywords = parseKeywords(violenceKeywordsCsv);
        this.sexualKeywords = parseKeywords(sexualKeywordsCsv);
        this.unsafeThreshold = Math.max(0.5d, Math.min(unsafeThreshold, 1.0d));
        this.reviewThreshold = Math.max(0.1d, Math.min(reviewThreshold, this.unsafeThreshold - 0.05d));
        this.awsMinConfidence = Math.max(1.0f, Math.min(awsMinConfidence, 100.0f));
        this.awsFailOpen = awsFailOpen;
    }

    @Override
    @Transactional
    public void moderateMedia(UUID mediaId) {
        log.info("Running content moderation for mediaId={} mode={}", mediaId, providerMode);
        MediaFile media = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new MediaFileNotFoundException(mediaId));

        ModerationResult result = evaluate(media);
        Map<String, Object> moderationMeta = new HashMap<>();
        moderationMeta.put("confidence", result.confidence());
        moderationMeta.put("reason", result.reason());
        moderationMeta.put("safety_status", result.safetyStatus());
        moderationMeta.put("timestamp", java.time.Instant.now());
        moderationMeta.put("provider_mode", providerMode);
        moderationMeta.put("raw_response", result.rawResponse());
        media.getMeta().put("moderation", moderationMeta);
        media.setSafetyStatus(result.safetyStatus());

        if ("unsafe".equalsIgnoreCase(result.safetyStatus())) {
            media.setState(MediaStatus.UNSAFE);
        } else if ("review".equalsIgnoreCase(result.safetyStatus())) {
            media.setState(MediaStatus.REVIEW);
        }
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

    private ModerationResult evaluate(MediaFile media) {
        if ("allow_all".equals(providerMode)) {
            return ModerationResult.safe();
        }
        if ("aws_rekognition".equals(providerMode)) {
            return evaluateWithAwsRekognition(media);
        }
        return evaluateByKeywords(media);
    }

    private ModerationResult evaluateWithAwsRekognition(MediaFile media) {
        if (!s3Enabled || s3Bucket == null || s3Bucket.isBlank()) {
            return ModerationResult.review(
                    1.0d,
                    "aws_rekognition mode requires S3-enabled storage",
                    Map.of("provider", "aws_rekognition", "s3_enabled", s3Enabled)
            );
        }
        if (rekognitionClient == null) {
            return ModerationResult.review(
                    1.0d,
                    "Rekognition client unavailable",
                    Map.of("provider", "aws_rekognition")
            );
        }
        try {
            DetectModerationLabelsResponse response = rekognitionClient.detectModerationLabels(
                    DetectModerationLabelsRequest.builder()
                            .image(Image.builder()
                                    .s3Object(S3Object.builder()
                                            .bucket(s3Bucket)
                                            .name(media.getStorageKey())
                                            .build())
                                    .build())
                            .minConfidence(awsMinConfidence)
                            .build()
            );

            List<ModerationLabel> labels = response.moderationLabels() != null ? response.moderationLabels() : List.of();
            if (labels.isEmpty()) {
                return ModerationResult.safe();
            }

            double top = 0.0d;
            List<String> categories = new ArrayList<>();
            List<Map<String, Object>> labelDump = new ArrayList<>();

            for (ModerationLabel label : labels) {
                double c = (label.confidence() != null ? label.confidence() : 0.0f) / 100.0d;
                top = Math.max(top, c);
                String mapped = mapAwsLabelToCategory(label);
                if (!"other".equals(mapped)) {
                    categories.add(mapped);
                }
                labelDump.add(Map.of(
                        "name", safe(label.name()),
                        "parent", safe(label.parentName()),
                        "confidence", c
                ));
            }

            String reason = "Detected sensitive labels: " + categories.stream().distinct().toList();
            Map<String, Object> raw = Map.of(
                    "provider", "aws_rekognition",
                    "labels", labelDump,
                    "top_confidence", top
            );
            if (top >= unsafeThreshold) {
                return ModerationResult.unsafe(top, reason, raw);
            }
            if (top >= reviewThreshold) {
                return ModerationResult.review(top, reason, raw);
            }
            return ModerationResult.safe();
        } catch (Exception ex) {
            log.error("AWS Rekognition moderation failed for mediaId={}, key={}", media.getId(), media.getStorageKey(), ex);
            if (awsFailOpen) {
                return ModerationResult.safe();
            }
            return ModerationResult.review(
                    1.0d,
                    "Moderation provider error",
                    Map.of("provider", "aws_rekognition", "error", ex.getClass().getSimpleName())
            );
        }
    }

    private ModerationResult evaluateByKeywords(MediaFile media) {
        String text = ((media.getFilename() == null ? "" : media.getFilename()) + " " +
                (media.getStorageKey() == null ? "" : media.getStorageKey())).toLowerCase(Locale.ROOT);
        List<String> matchedCategories = new ArrayList<>();
        List<String> matchedKeywords = new ArrayList<>();
        double score = 0.0d;

        score = Math.max(score, collectMatches("hate", text, hateKeywords, matchedCategories, matchedKeywords, 0.7d));
        score = Math.max(score, collectMatches("violence", text, violenceKeywords, matchedCategories, matchedKeywords, 0.8d));
        score = Math.max(score, collectMatches("sexual", text, sexualKeywords, matchedCategories, matchedKeywords, 0.9d));

        if (score >= unsafeThreshold) {
            return ModerationResult.unsafe(
                    score,
                    "Matched unsafe keywords for categories: " + String.join(",", matchedCategories),
                    Map.of("categories", matchedCategories, "keywords", matchedKeywords)
            );
        }
        if (score >= reviewThreshold) {
            return ModerationResult.review(
                    score,
                    "Matched sensitive keywords for categories: " + String.join(",", matchedCategories),
                    Map.of("categories", matchedCategories, "keywords", matchedKeywords)
            );
        }
        return ModerationResult.safe();
    }

    private double collectMatches(
            String category,
            String text,
            List<String> keywords,
            List<String> matchedCategories,
            List<String> matchedKeywords,
            double categoryScore) {
        for (String keyword : keywords) {
            if (!keyword.isBlank() && text.contains(keyword)) {
                matchedCategories.add(category);
                matchedKeywords.add(keyword);
                return categoryScore;
            }
        }
        return 0.0d;
    }

    private List<String> parseKeywords(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(s -> s == null ? "" : s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }

    private String mapAwsLabelToCategory(ModerationLabel label) {
        String name = (safe(label.name()) + " " + safe(label.parentName())).toLowerCase(Locale.ROOT);
        if (containsAny(name, "hate", "harassment", "extremism")) {
            return "hate";
        }
        if (containsAny(name, "violence", "graphic", "weapon", "blood", "injury")) {
            return "violence";
        }
        if (containsAny(name, "nudity", "sexual", "explicit", "suggestive")) {
            return "sexual";
        }
        return "other";
    }

    private boolean containsAny(String haystack, String... needles) {
        for (String n : needles) {
            if (haystack.contains(n)) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
