package com.travelo.mediaservice.reel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis-backed tracker for multi-node production use.
 * <p>
 * Each job is stored as a hash at {@code reel:job:{jobId}}:
 * <pre>
 *     stage      -&gt; ReelJobStage name
 *     percent    -&gt; int (0..100), optional
 *     message    -&gt; string, optional
 *     updatedAt  -&gt; ISO-8601 Instant
 * </pre>
 * Activated by {@code reel.progress.backend=redis}; wired in
 * {@link ReelProgressTrackerConfig}.
 */
public class RedisReelJobProgressTracker implements ReelJobProgressTracker {

    private static final Logger log = LoggerFactory.getLogger(RedisReelJobProgressTracker.class);
    private static final Duration TTL = Duration.ofMinutes(15);
    private static final String KEY_PREFIX = "reel:job:";

    private static final String FIELD_STAGE = "stage";
    private static final String FIELD_PERCENT = "percent";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_UPDATED_AT = "updatedAt";

    private final StringRedisTemplate redisTemplate;
    private final ReelJobProgressBroker broker;

    public RedisReelJobProgressTracker(StringRedisTemplate redisTemplate) {
        this(redisTemplate, null);
    }

    public RedisReelJobProgressTracker(StringRedisTemplate redisTemplate, ReelJobProgressBroker broker) {
        this.redisTemplate = redisTemplate;
        this.broker = broker;
    }

    @Override
    public void report(String jobId, ReelJobStage stage, String message, Integer percent) {
        if (jobId == null || jobId.isBlank() || stage == null) {
            return;
        }
        try {
            Integer clamped = percent == null ? null : Math.max(0, Math.min(100, percent));
            String key = key(jobId);
            HashOperations<String, Object, Object> hash = redisTemplate.opsForHash();

            // Monotonic percent within the same stage: read-modify-write is sufficient
            // since per-job writes happen from a single processing thread.
            Snapshot existing = readSnapshot(key);
            if (existing != null && existing.stage() == stage
                    && existing.percent() != null && clamped != null
                    && clamped < existing.percent()) {
                clamped = existing.percent();
            }

            Map<String, String> fields = new HashMap<>();
            fields.put(FIELD_STAGE, stage.name());
            fields.put(FIELD_UPDATED_AT, Instant.now().toString());
            if (message != null && !message.isEmpty()) {
                fields.put(FIELD_MESSAGE, message);
            }
            if (clamped != null) {
                fields.put(FIELD_PERCENT, clamped.toString());
            }

            @SuppressWarnings({"unchecked", "rawtypes"})
            Map<Object, Object> erased = (Map) fields;
            hash.putAll(key, erased);
            redisTemplate.expire(key, TTL);

            if (broker != null) {
                Instant updatedAt;
                try {
                    updatedAt = Instant.parse(fields.get(FIELD_UPDATED_AT));
                } catch (Exception e) {
                    updatedAt = Instant.now();
                }
                broker.publish(jobId, new Snapshot(stage, message, clamped, updatedAt));
            }
        } catch (Exception ex) {
            // Never let tracker issues break the processing pipeline.
            log.warn("redis tracker report failed jobId={} stage={}: {}", jobId, stage, ex.toString());
        }
    }

    @Override
    public Snapshot get(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return null;
        }
        try {
            return readSnapshot(key(jobId));
        } catch (Exception ex) {
            log.warn("redis tracker get failed jobId={}: {}", jobId, ex.toString());
            return null;
        }
    }

    private Snapshot readSnapshot(String key) {
        HashOperations<String, Object, Object> hash = redisTemplate.opsForHash();
        Map<Object, Object> raw = hash.entries(key);
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        ReelJobStage stage = parseStage(asString(raw.get(FIELD_STAGE)));
        if (stage == null) {
            return null;
        }
        Integer percent = parseInt(asString(raw.get(FIELD_PERCENT)));
        String message = asString(raw.get(FIELD_MESSAGE));
        Instant updatedAt = parseInstantOrNow(asString(raw.get(FIELD_UPDATED_AT)));
        return new Snapshot(stage, message, percent, updatedAt);
    }

    private static String key(String jobId) {
        return KEY_PREFIX + jobId;
    }

    private static String asString(Object v) {
        return v == null ? null : v.toString();
    }

    private static ReelJobStage parseStage(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return ReelJobStage.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Integer parseInt(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Instant parseInstantOrNow(String s) {
        if (s == null || s.isEmpty()) return Instant.now();
        try {
            return Instant.parse(s);
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
