package com.travelo.momentsservice.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelo.momentsservice.config.MomentsStorageProperties;
import com.travelo.momentsservice.dto.MomentAiSuggestionResponse;
import com.travelo.momentsservice.dto.MomentCommentResponse;
import com.travelo.momentsservice.dto.MomentCreateResponse;
import com.travelo.momentsservice.dto.MomentDetailsResponse;
import com.travelo.momentsservice.dto.MomentFeedItemResponse;
import com.travelo.momentsservice.dto.MomentLikeResponse;
import com.travelo.momentsservice.engagement.MomentEngagementStore;
import com.travelo.momentsservice.model.MomentRecord;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

@Service
public class MomentsServiceImpl implements MomentsService {

    private static final Logger log = LoggerFactory.getLogger(MomentsServiceImpl.class);

    private final MomentsStorageProperties storageProperties;
    private final ObjectMapper objectMapper;
    private final MomentEngagementStore engagementStore;
    private final CopyOnWriteArrayList<MomentRecord> records = new CopyOnWriteArrayList<>();
    private final Path recordsFilePath;

    public MomentsServiceImpl(
            MomentsStorageProperties storageProperties,
            ObjectMapper objectMapper,
            MomentEngagementStore engagementStore
    ) {
        this.storageProperties = storageProperties;
        this.objectMapper = objectMapper;
        this.engagementStore = engagementStore;
        this.recordsFilePath = Path.of(storageProperties.localDir(), "moments-records.json");
        loadRecordsFromDisk();
    }

    @Override
    public MomentCreateResponse createMoment(
            String userId,
            String userName,
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
                        (userId == null || userId.isBlank()) ? "current-user" : userId,
                        (userName == null || userName.isBlank()) ? "You" : userName,
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

        return new MomentCreateResponse(
                momentId,
                true,
                (userId == null || userId.isBlank()) ? "current-user" : userId,
                (userName == null || userName.isBlank()) ? "You" : userName,
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
            String tags
    ) {
        final String safeLocation = (location == null || location.isBlank()) ? "Fort Kochi" : location;
        final String normalized = action == null ? "caption" : action.trim().toLowerCase();
        final List<Double> scenes = List.of(6d, 13d, 21d, 29d, 37d, 46d, 54d);
        final String highlightsJson = "[{\"start\":8,\"end\":14,\"highlight\":true},{\"start\":24,\"end\":31,\"highlight\":true},{\"start\":42,\"end\":49,\"highlight\":true}]";
        final String segmentsJson = "[{\"start\":8,\"end\":14,\"highlight\":true},{\"start\":24,\"end\":31,\"highlight\":true}]";

        String generatedCaption = caption == null ? "" : caption;
        String generatedFilter = "Cinematic";
        List<String> generatedTags = List.of("sunset", "travel", "hidden-gem");
        if ("caption".equals(normalized) || generatedCaption.isBlank()) {
            generatedCaption = "Golden sunset vibes at " + safeLocation + " 🌅";
        }
        if ("music-sync".equals(normalized)) {
            generatedFilter = "Vibrant";
            generatedTags = List.of("beat-sync", "travel-edit", "cinematic");
        } else if ("scenes".equals(normalized)) {
            generatedFilter = "Original";
        }

        return new MomentAiSuggestionResponse(
                normalized,
                generatedCaption,
                generatedTags,
                generatedFilter,
                scenes,
                segmentsJson,
                highlightsJson
        );
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
}
