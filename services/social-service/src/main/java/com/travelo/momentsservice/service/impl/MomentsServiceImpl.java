package com.travelo.momentsservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelo.momentsservice.ai.MomentAiLlmCaptionPack;
import com.travelo.momentsservice.ai.MomentAiLlmRequest;
import com.travelo.momentsservice.ai.MomentAiTimelinePlanner;
import com.travelo.momentsservice.ai.OpenAiMomentAiEnrichmentService;
import com.travelo.momentsservice.config.MomentsStorageProperties;
import com.travelo.momentsservice.dto.MomentAiSuggestionResponse;
import com.travelo.momentsservice.dto.MomentCommentResponse;
import com.travelo.momentsservice.dto.MomentCreateResponse;
import com.travelo.momentsservice.dto.MomentDetailsResponse;
import com.travelo.momentsservice.dto.MomentFeedItemResponse;
import com.travelo.momentsservice.dto.MomentLikeResponse;
import com.travelo.momentsservice.engagement.MomentEngagementStore;
import com.travelo.momentsservice.model.MomentRecord;
import com.travelo.momentsservice.model.MomentIdempotencyRecord;
import com.travelo.momentsservice.service.MomentsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

@Service
public class MomentsServiceImpl implements MomentsService {

    private static final Logger log = LoggerFactory.getLogger(MomentsServiceImpl.class);

    private static final Set<String> MOMENT_AI_VIDEO_FILTERS = Set.of(
            "Original", "Warm", "Cinematic", "Vibrant", "B&W", "Cool"
    );

    private final MomentsStorageProperties storageProperties;
    private final ObjectMapper objectMapper;
    private final MomentEngagementStore engagementStore;
    private final OpenAiMomentAiEnrichmentService openAiMomentAiEnrichment;
    private final CopyOnWriteArrayList<MomentRecord> records = new CopyOnWriteArrayList<>();
    private final Map<String, MomentIdempotencyRecord> idempotencyRecords = new ConcurrentHashMap<>();
    private final Path recordsFilePath;
    private final Path idempotencyFilePath;
    private final Duration idempotencyTtl;

    public MomentsServiceImpl(
            MomentsStorageProperties storageProperties,
            ObjectMapper objectMapper,
            MomentEngagementStore engagementStore,
            OpenAiMomentAiEnrichmentService openAiMomentAiEnrichment,
            @org.springframework.beans.factory.annotation.Value("${moments.idempotency.ttl-hours:24}") long idempotencyTtlHours
    ) {
        this.storageProperties = storageProperties;
        this.objectMapper = objectMapper;
        this.engagementStore = engagementStore;
        this.openAiMomentAiEnrichment = openAiMomentAiEnrichment;
        this.recordsFilePath = Path.of(storageProperties.localDir(), "moments-records.json");
        this.idempotencyFilePath = Path.of(storageProperties.localDir(), "moments-idempotency.json");
        this.idempotencyTtl = Duration.ofHours(Math.max(1, idempotencyTtlHours));
        loadRecordsFromDisk();
        loadIdempotencyFromDisk();
    }

    @Override
    public MomentCreateResponse createMoment(
            String userId,
            String userName,
            String idempotencyKey,
            String type,
            String mediaType,
            String caption,
            String location,
            List<String> tags,
            List<String> mediaUrls,
            String thumbnailPath,
            Double trimStart,
            Double trimEnd,
            String videoFilter,
            String cropPreset,
            String musicUrl,
            String musicName,
            Double musicStart,
            boolean aiEnhanced,
            String segmentsJson,
            String highlightsJson,
            String scenesJson,
            String mediaDurationsJson,
            String editorMetadataJson,
            String audience,
            List<MultipartFile> files
    ) throws IOException {
        final String normalizedUserId = (userId == null || userId.isBlank()) ? "current-user" : userId;
        final String normalizedUserName = (userName == null || userName.isBlank()) ? "You" : userName;
        final String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        if (normalizedIdempotencyKey != null) {
            final MomentCreateResponse replay = replayIdempotentResponse(normalizedIdempotencyKey, normalizedUserId);
            if (replay != null) {
                log.info("flow=moment_create idempotency_replay key={} userId={} momentId={}",
                        normalizedIdempotencyKey, normalizedUserId, replay.id());
                return replay;
            }
        }

        final String momentId = "moment_" + System.currentTimeMillis();
        log.info(
                "flow=moment_create service begin momentId={} type={} mediaType={} rawUrlCount={} multipartCount={}",
                momentId,
                type,
                mediaType,
                mediaUrls != null ? mediaUrls.size() : 0,
                files != null ? files.size() : 0);
        final Path baseDir = Path.of(storageProperties.localDir(), momentId);
        Files.createDirectories(baseDir);

        final List<String> storedFiles = new ArrayList<>();
        if (mediaUrls != null) {
            for (String mediaUrl : mediaUrls) {
                if (isValidHttpUrl(mediaUrl)) {
                    storedFiles.add(mediaUrl.trim());
                }
            }
        }
        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                final String filename = resolveFilename(file);
                final Path target = baseDir.resolve(filename);
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                storedFiles.add(target.toString());
            }
        }
        if (storedFiles.isEmpty()) {
            if (allowsTextOnlyMoment(type, caption)) {
                log.info(
                        "flow=moment_create text_only momentId={} type={} captionLen={}",
                        momentId,
                        type,
                        caption != null ? caption.length() : 0);
            } else {
                log.warn(
                        "flow=moment_create reject momentId={} reason=no_valid_media afterAcceptUrls={} (invalid or empty URLs)",
                        momentId,
                        mediaUrls != null ? mediaUrls.size() : 0);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one valid media URL or file is required");
            }
        }
        final OffsetDateTime createdAt = OffsetDateTime.now();
        records.add(
                new MomentRecord(
                        momentId,
                        normalizedUserId,
                        normalizedUserName,
                        type,
                        mediaType,
                        caption,
                        location,
                        tags,
                        thumbnailPath,
                        trimStart,
                        trimEnd,
                        videoFilter,
                        cropPreset,
                        musicUrl,
                        musicName,
                        musicStart,
                        aiEnhanced,
                        segmentsJson,
                        highlightsJson,
                        scenesJson,
                        mediaDurationsJson,
                        editorMetadataJson,
                        (audience == null || audience.isBlank()) ? "followers" : audience,
                        List.copyOf(storedFiles),
                        createdAt
                )
        );
        persistRecordsToDisk();

        log.info("flow=moment_create service done momentId={} storedCount={} localDir={}",
                momentId, storedFiles.size(), baseDir);

        final MomentCreateResponse response = new MomentCreateResponse(
                momentId,
                true,
                normalizedUserId,
                normalizedUserName,
                type,
                mediaType,
                caption,
                location,
                tags,
                thumbnailPath,
                trimStart,
                trimEnd,
                videoFilter,
                cropPreset,
                musicUrl,
                musicName,
                musicStart,
                aiEnhanced,
                segmentsJson,
                highlightsJson,
                scenesJson,
                mediaDurationsJson,
                editorMetadataJson,
                audience,
                storedFiles,
                createdAt
        );
        if (normalizedIdempotencyKey != null) {
            storeIdempotentResponse(normalizedIdempotencyKey, normalizedUserId, response);
        }
        return response;
    }

    @Override
    public MomentDetailsResponse getMoment(String momentId) throws IOException {
        final Path momentDir = Path.of(storageProperties.localDir(), momentId);
        if (!Files.exists(momentDir) || !Files.isDirectory(momentDir)) {
            return new MomentDetailsResponse(
                    momentId,
                    List.of(),
                    "image",
                    "",
                    "",
                    List.of(),
                    "",
                    null,
                    null,
                    "",
                    "",
                    "",
                    "",
                    null,
                    false,
                    "[]",
                    "[]",
                    "[]",
                    "[]"
            );
        }
        try (Stream<Path> stream = Files.list(momentDir)) {
            final List<String> files = stream
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .toList();
            final MomentRecord record = records.stream()
                    .filter(r -> r.id().equals(momentId))
                    .findFirst()
                    .orElse(null);
            if (record == null) {
                return new MomentDetailsResponse(
                        momentId,
                        files,
                        "image",
                        "",
                        "",
                        List.of(),
                        "",
                        null,
                        null,
                        "",
                        "",
                        "",
                        "",
                        null,
                        false,
                        "[]",
                        "[]",
                        "[]",
                        "[]"
                );
            }
            return new MomentDetailsResponse(
                    momentId,
                    record.storedFiles().isEmpty() ? files : record.storedFiles(),
                    record.mediaType(),
                    record.caption(),
                    record.location(),
                    record.tags(),
                    record.thumbnailPath(),
                    record.trimStart(),
                    record.trimEnd(),
                    record.videoFilter(),
                    record.cropPreset(),
                    record.musicUrl(),
                    record.musicName(),
                    record.musicStart(),
                    record.aiEnhanced(),
                    record.segmentsJson(),
                    record.highlightsJson(),
                    record.scenesJson(),
                    record.mediaDurationsJson()
            );
        }
    }

    @Override
    public List<MomentFeedItemResponse> getFeed(String baseUrl, int limit, String viewerUserId) {
        final int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        return records.stream()
                .sorted(Comparator.comparing(MomentRecord::createdAt).reversed())
                .limit(safeLimit)
                .map(record -> {
                    final String mediaUrl = resolveMediaUrl(baseUrl, record);
                    final String thumbnailUrl = record.thumbnailPath() != null && !record.thumbnailPath().isBlank()
                            ? record.thumbnailPath()
                            : mediaUrl;
                    final String mid = record.id();
                    return new MomentFeedItemResponse(
                            mid,
                            record.userId(),
                            record.userName(),
                            record.caption(),
                            record.location(),
                            record.type(),
                            record.mediaType(),
                            thumbnailUrl,
                            record.musicName(),
                            record.aiEnhanced(),
                            mediaUrl,
                            record.createdAt(),
                            engagementStore.likeCount(mid),
                            engagementStore.commentCount(mid),
                            engagementStore.isLiked(mid, viewerUserId),
                            engagementStore.isViewed(mid, viewerUserId)
                    );
                })
                .toList();
    }

    @Override
    public MomentLikeResponse toggleMomentLike(String momentId, String userId) {
        return engagementStore.toggleLike(momentId, userId);
    }

    @Override
    public void recordMomentView(String momentId, String userId) {
        engagementStore.recordView(momentId, userId);
    }

    @Override
    public List<MomentCommentResponse> listMomentComments(String momentId) {
        return engagementStore.listComments(momentId);
    }

    @Override
    public MomentCommentResponse addMomentComment(
            String momentId,
            String userId,
            String userName,
            String commentText
    ) {
        return engagementStore.addComment(momentId, userId, userName, commentText);
    }

    @Override
    public Resource getMomentFile(String momentId, String fileName) throws IOException {
        final Path safePath = Path.of(storageProperties.localDir(), momentId, fileName).normalize();
        final Path baseDir = Path.of(storageProperties.localDir()).normalize();
        if (!safePath.startsWith(baseDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
        }
        if (!Files.exists(safePath) || !Files.isRegularFile(safePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        return new PathResource(safePath);
    }

    private String resolveMediaUrl(String baseUrl, MomentRecord record) {
        if (record.storedFiles().isEmpty()) {
            return "";
        }
        final String first = record.storedFiles().getFirst();
        if (isValidHttpUrl(first)) {
            return first;
        }
        try {
            return baseUrl + "/" + record.id() + "/files/" + Path.of(first).getFileName();
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean isValidHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            final URI uri = new URI(value.trim());
            final String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (URISyntaxException ignored) {
            return false;
        }
    }

    @Override
    public MomentAiSuggestionResponse suggestAiEdits(
            String action,
            String caption,
            String location,
            String tags,
            Double durationSec
    ) {
        final String normalized = action == null ? "caption" : action.trim().toLowerCase(Locale.ROOT);
        final String safeLocation = (location == null || location.isBlank()) ? "Fort Kochi" : location.strip();
        final double dur = MomentAiTimelinePlanner.clampDurationSeconds(durationSec);
        final List<String> parsedTags = parseCommaTags(tags);

        final List<Map<String, Object>> highlightRows = MomentAiTimelinePlanner.buildHighlights(dur);
        final List<Double> sceneTimes = MomentAiTimelinePlanner.buildSceneTimes(dur);

        final List<Map<String, Object>> segmentRows;
        if ("music-sync".equals(normalized)) {
            List<Map<String, Object>> src = MomentAiTimelinePlanner.firstSegments(highlightRows, 4);
            if (src.isEmpty()) {
                src = highlightRows;
            }
            segmentRows = MomentAiTimelinePlanner.quantizeSegments(src, dur);
        } else {
            segmentRows = MomentAiTimelinePlanner.firstSegments(highlightRows, 2);
        }

        final String highlightsJson;
        final String segmentsJson;
        try {
            highlightsJson = objectMapper.writeValueAsString(highlightRows);
            segmentsJson = objectMapper.writeValueAsString(segmentRows);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("moment ai timeline json", e);
        }

        final String generatedCaption = buildMomentAiCaption(normalized, caption, safeLocation, parsedTags, dur);
        final String generatedFilter = resolveMomentAiVideoFilter(normalized);
        final List<String> generatedTags = mergeMomentAiTags(parsedTags, momentAiTagBoosters(normalized));

        final MomentAiSuggestionResponse heuristic = new MomentAiSuggestionResponse(
                normalized,
                generatedCaption,
                generatedTags,
                generatedFilter,
                sceneTimes,
                segmentsJson,
                highlightsJson
        );

        Optional<MomentAiLlmCaptionPack> llm = openAiMomentAiEnrichment.maybeEnrich(
                new MomentAiLlmRequest(normalized, caption, safeLocation, parsedTags, dur)
        );
        return llm.map(p -> new MomentAiSuggestionResponse(
                normalized,
                p.caption(),
                mergeMomentAiTagsWithLlm(parsedTags, momentAiTagBoosters(normalized), p.tags()),
                sanitizeMomentAiVideoFilter(p.videoFilter(), generatedFilter),
                sceneTimes,
                segmentsJson,
                highlightsJson
        )).orElse(heuristic);
    }

    private static String sanitizeMomentAiVideoFilter(String fromLlm, String heuristic) {
        if (!StringUtils.hasText(fromLlm)) {
            return heuristic;
        }
        String t = fromLlm.trim();
        return MOMENT_AI_VIDEO_FILTERS.contains(t) ? t : heuristic;
    }

    private static List<String> mergeMomentAiTagsWithLlm(List<String> parsed, String[] boosters, List<String> llmTags) {
        LinkedHashSet<String> set = new LinkedHashSet<>(mergeMomentAiTags(parsed, boosters));
        if (llmTags != null) {
            for (String raw : llmTags) {
                if (!StringUtils.hasText(raw) || set.size() >= 20) {
                    continue;
                }
                set.add(raw.trim().toLowerCase(Locale.ROOT));
            }
        }
        return new ArrayList<>(set);
    }

    private static List<String> parseCommaTags(String raw) {
        if (!StringUtils.hasText(raw)) {
            return new ArrayList<>();
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            String t = part.trim();
            if (StringUtils.hasText(t) && out.size() < 16) {
                out.add(t);
            }
        }
        return out;
    }

    private static List<String> mergeMomentAiTags(List<String> base, String[] extras) {
        LinkedHashSet<String> set = new LinkedHashSet<>(base);
        for (String e : extras) {
            if (StringUtils.hasText(e)) {
                set.add(e.trim());
            }
        }
        return new ArrayList<>(set);
    }

    private static String[] momentAiTagBoosters(String normalized) {
        return switch (normalized) {
            case "music-sync" -> new String[]{"beat-sync", "travel-edit", "cinematic"};
            case "highlights" -> new String[]{"best-moments", "travel"};
            case "scenes" -> new String[]{"scene-cuts"};
            case "smart-trim" -> new String[]{"smart-cut"};
            case "studio_suggest" -> new String[]{"studio-grade", "travel"};
            default -> new String[]{"travel", "moments"};
        };
    }

    private static String resolveMomentAiVideoFilter(String normalized) {
        return switch (normalized) {
            case "scenes" -> "Original";
            case "music-sync" -> "Vibrant";
            case "smart-trim", "studio_suggest" -> "Warm";
            case "highlights" -> "Cinematic";
            default -> "Cinematic";
        };
    }

    private static String shortenPlace(String location, int max) {
        if (!StringUtils.hasText(location)) {
            return "this spot";
        }
        String t = location.strip();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }

    private static String buildMomentAiCaption(
            String normalized,
            String userCaption,
            String location,
            List<String> tagList,
            double durSeconds
    ) {
        final String place = shortenPlace(location, 48);
        final boolean hasUser = StringUtils.hasText(userCaption);
        final String topic = !tagList.isEmpty() ? tagList.get(0) : "travel";

        return switch (normalized) {
            case "caption" -> hasUser
                    ? "Try leading with emotion, then anchor at " + place + " — detail beats generic praise."
                    : "Golden light around " + place + " — worth slowing down for 🌅";
            case "highlights" -> "Three strong beats: arrival, reaction, and a wide shot near " + place + ".";
            case "scenes" -> "Scene markers follow natural pauses across your ~" + Math.round(durSeconds) + "s clip at "
                    + place + ".";
            case "smart-trim" -> "Hook in the first highlight; land the payoff before the last "
                    + Math.max(2, Math.round(durSeconds * 0.12)) + "s at " + place + ".";
            case "music-sync" -> "Snap cuts on the 0.25s grid; ride the beat through " + place + ".";
            case "studio_suggest" -> hasUser
                    ? "Studio polish: keep " + topic + " energy — balance skies and skin tones at " + place + "."
                    : "Warm grade + honest line about " + place + " — one hook, one specific detail.";
            default -> hasUser
                    ? "Refine pacing at " + place + " — " + userCaption.substring(0, Math.min(72, userCaption.length()))
                    : "Moments that breathe — start at " + place + ".";
        };
    }

    /** Text-only posts (e.g. Flutter entry mode {@code writeTip}) — caption must be non-blank. */
    private static boolean allowsTextOnlyMoment(String type, String caption) {
        if (!StringUtils.hasText(caption)) {
            return false;
        }
        if (!StringUtils.hasText(type)) {
            return false;
        }
        final String t = type.trim();
        return t.equalsIgnoreCase("writeTip")
                || t.equalsIgnoreCase("tip")
                || t.equalsIgnoreCase("text");
    }

    private String resolveFilename(MultipartFile file) {
        final String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null
                ? "moment-file"
                : file.getOriginalFilename());
        final String ext = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.'))
                : "";
        return UUID.randomUUID() + ext;
    }

    private void loadRecordsFromDisk() {
        try {
            Files.createDirectories(recordsFilePath.getParent());
            if (!Files.exists(recordsFilePath)) {
                return;
            }
            final List<MomentRecord> restored = objectMapper.readValue(
                    recordsFilePath.toFile(),
                    new TypeReference<>() {
                    }
            );
            records.clear();
            records.addAll(restored);
            log.info("flow=moment_restore restoredCount={} file={}", records.size(), recordsFilePath);
        } catch (Exception e) {
            log.warn("flow=moment_restore failed file={} err={}", recordsFilePath, e.toString());
        }
    }

    private void persistRecordsToDisk() {
        try {
            Files.createDirectories(recordsFilePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(recordsFilePath.toFile(), records);
        } catch (Exception e) {
            log.warn("flow=moment_persist failed file={} err={}", recordsFilePath, e.toString());
        }
    }

    private String normalizeIdempotencyKey(String key) {
        if (key == null) {
            return null;
        }
        final String k = key.trim();
        if (k.isBlank() || k.length() > 200) {
            return null;
        }
        return k;
    }

    private String idempotencyCompositeKey(String key, String userId) {
        return userId + "::" + key;
    }

    private MomentCreateResponse replayIdempotentResponse(String key, String userId) {
        pruneExpiredIdempotencyKeys();
        final MomentIdempotencyRecord record = idempotencyRecords.get(idempotencyCompositeKey(key, userId));
        if (record == null) {
            return null;
        }
        return record.response();
    }

    private void storeIdempotentResponse(String key, String userId, MomentCreateResponse response) {
        pruneExpiredIdempotencyKeys();
        final MomentIdempotencyRecord record = new MomentIdempotencyRecord(
                key,
                userId,
                response,
                OffsetDateTime.now()
        );
        idempotencyRecords.put(idempotencyCompositeKey(key, userId), record);
        persistIdempotencyToDisk();
    }

    private void pruneExpiredIdempotencyKeys() {
        final OffsetDateTime cutoff = OffsetDateTime.now().minus(idempotencyTtl);
        boolean changed = false;
        final var it = idempotencyRecords.entrySet().iterator();
        while (it.hasNext()) {
            final var e = it.next();
            final MomentIdempotencyRecord r = e.getValue();
            if (r == null || r.createdAt() == null || r.createdAt().isBefore(cutoff)) {
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            persistIdempotencyToDisk();
        }
    }

    private void loadIdempotencyFromDisk() {
        try {
            Files.createDirectories(idempotencyFilePath.getParent());
            if (!Files.exists(idempotencyFilePath)) {
                return;
            }
            final List<MomentIdempotencyRecord> restored = objectMapper.readValue(
                    idempotencyFilePath.toFile(),
                    new TypeReference<>() {
                    }
            );
            idempotencyRecords.clear();
            for (MomentIdempotencyRecord r : restored) {
                if (r == null || r.key() == null || r.userId() == null) {
                    continue;
                }
                idempotencyRecords.put(idempotencyCompositeKey(r.key(), r.userId()), r);
            }
            pruneExpiredIdempotencyKeys();
            log.info("flow=moment_idempotency_restore restoredCount={} file={}",
                    idempotencyRecords.size(), idempotencyFilePath);
        } catch (Exception e) {
            log.warn("flow=moment_idempotency_restore failed file={} err={}", idempotencyFilePath, e.toString());
        }
    }

    private void persistIdempotencyToDisk() {
        try {
            Files.createDirectories(idempotencyFilePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(idempotencyFilePath.toFile(), List.copyOf(idempotencyRecords.values()));
        } catch (Exception e) {
            log.warn("flow=moment_idempotency_persist failed file={} err={}", idempotencyFilePath, e.toString());
        }
    }
}
