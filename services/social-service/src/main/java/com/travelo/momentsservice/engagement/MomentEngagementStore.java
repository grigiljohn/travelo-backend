package com.travelo.momentsservice.engagement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelo.momentsservice.config.MomentsStorageProperties;
import com.travelo.momentsservice.dto.MomentCommentResponse;
import com.travelo.momentsservice.dto.MomentLikeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory engagement (likes, views, comments) for moments with JSON persistence.
 */
@Component
public class MomentEngagementStore {

    private static final Logger log = LoggerFactory.getLogger(MomentEngagementStore.class);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ObjectMapper objectMapper;
    private final Path engagementFile;
    private final ConcurrentHashMap<String, MomentBucket> buckets = new ConcurrentHashMap<>();

    public MomentEngagementStore(MomentsStorageProperties storageProperties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.engagementFile = Path.of(storageProperties.localDir(), "moments-engagement.json");
        loadFromDisk();
    }

    public int likeCount(String momentId) {
        return bucket(momentId).likes.size();
    }

    public int commentCount(String momentId) {
        return bucket(momentId).comments.size();
    }

    public boolean isLiked(String momentId, String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        return bucket(momentId).likes.contains(userId.trim());
    }

    public boolean isViewed(String momentId, String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        return bucket(momentId).views.contains(userId.trim());
    }

    /**
     * Toggle like for user; returns new like count and whether user now likes the moment.
     */
    public MomentLikeResponse toggleLike(String momentId, String userId) {
        if (userId == null || userId.isBlank()) {
            return new MomentLikeResponse(likeCount(momentId), false);
        }
        final MomentBucket b = bucket(momentId);
        synchronized (b) {
            if (b.likes.contains(userId)) {
                b.likes.remove(userId);
            } else {
                b.likes.add(userId);
            }
        }
        persist();
        return new MomentLikeResponse(b.likes.size(), b.likes.contains(userId));
    }

    public void recordView(String momentId, String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        final MomentBucket b = bucket(momentId);
        synchronized (b) {
            if (!b.views.contains(userId)) {
                b.views.add(userId);
            }
        }
        persist();
    }

    public List<MomentCommentResponse> listComments(String momentId) {
        final MomentBucket b = bucket(momentId);
        synchronized (b) {
            return new ArrayList<>(b.comments);
        }
    }

    public MomentCommentResponse addComment(String momentId, String userId, String userName, String text) {
        final MomentBucket b = bucket(momentId);
        final String id = UUID.randomUUID().toString();
        final String created = OffsetDateTime.now().format(ISO);
        final String safeName = (userName == null || userName.isBlank()) ? "Traveler" : userName.trim();
        final MomentCommentResponse row = new MomentCommentResponse(
                id,
                momentId,
                userId == null ? "" : userId,
                safeName,
                text,
                created
        );
        synchronized (b) {
            b.comments.add(row);
        }
        persist();
        return row;
    }

    private MomentBucket bucket(String momentId) {
        final String key = momentId == null ? "" : momentId.trim();
        return buckets.computeIfAbsent(key, k -> new MomentBucket());
    }

    private void loadFromDisk() {
        try {
            Files.createDirectories(engagementFile.getParent());
            if (!Files.exists(engagementFile)) {
                return;
            }
            final Map<String, PersistedBucket> raw = objectMapper.readValue(
                    engagementFile.toFile(),
                    new TypeReference<>() {
                    }
            );
            buckets.clear();
            for (Map.Entry<String, PersistedBucket> e : raw.entrySet()) {
                final MomentBucket b = new MomentBucket();
                if (e.getValue().likes != null) {
                    b.likes.addAll(e.getValue().likes);
                }
                if (e.getValue().views != null) {
                    b.views.addAll(e.getValue().views);
                }
                if (e.getValue().comments != null) {
                    b.comments.addAll(e.getValue().comments);
                }
                buckets.put(e.getKey(), b);
            }
            log.info("flow=moment_engagement_load count={} file={}", buckets.size(), engagementFile);
        } catch (Exception ex) {
            log.warn("flow=moment_engagement_load_failed file={} err={}", engagementFile, ex.toString());
        }
    }

    private void persist() {
        try {
            Files.createDirectories(engagementFile.getParent());
            final Map<String, PersistedBucket> out = new LinkedHashMap<>();
            for (Map.Entry<String, MomentBucket> e : buckets.entrySet()) {
                final MomentBucket b = e.getValue();
                synchronized (b) {
                    out.put(e.getKey(), new PersistedBucket(
                            new ArrayList<>(b.likes),
                            new ArrayList<>(b.views),
                            new ArrayList<>(b.comments)
                    ));
                }
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(engagementFile.toFile(), out);
        } catch (Exception ex) {
            log.warn("flow=moment_engagement_persist_failed err={}", ex.toString());
        }
    }

    private static final class MomentBucket {
        private final List<String> likes = new ArrayList<>();
        private final List<String> views = new ArrayList<>();
        private final List<MomentCommentResponse> comments = new ArrayList<>();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static final class PersistedBucket {
        public List<String> likes;
        public List<String> views;
        public List<MomentCommentResponse> comments;

        @SuppressWarnings("unused")
        public PersistedBucket() {
        }

        PersistedBucket(List<String> likes, List<String> views, List<MomentCommentResponse> comments) {
            this.likes = likes;
            this.views = views;
            this.comments = comments;
        }
    }
}
