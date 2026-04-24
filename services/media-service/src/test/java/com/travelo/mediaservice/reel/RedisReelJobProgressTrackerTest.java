package com.travelo.mediaservice.reel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Fake-backed tests for {@link RedisReelJobProgressTracker}. Uses a Mockito
 * {@link StringRedisTemplate} whose {@code opsForHash()} is wired to an
 * in-memory {@code Map<String, Map<String,String>>} so we can exercise
 * real read-modify-write logic without touching a Redis instance.
 */
class RedisReelJobProgressTrackerTest {

    private StringRedisTemplate template;
    private Map<String, Map<String, String>> backing;
    private RedisReelJobProgressTracker tracker;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        template = mock(StringRedisTemplate.class);
        HashOperations hash = mock(HashOperations.class);
        backing = new HashMap<>();

        when(template.opsForHash()).thenReturn(hash);

        when(hash.entries(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0, String.class);
            Map<String, String> m = backing.get(key);
            if (m == null) {
                return new LinkedHashMap<>();
            }
            return new LinkedHashMap<Object, Object>(m);
        });

        doAnswer(inv -> {
            String key = inv.getArgument(0, String.class);
            Map<Object, Object> payload = inv.getArgument(1, Map.class);
            Map<String, String> coerced = new LinkedHashMap<>();
            for (Map.Entry<Object, Object> e : payload.entrySet()) {
                coerced.put(e.getKey().toString(), e.getValue().toString());
            }
            backing.put(key, coerced);
            return null;
        }).when(hash).putAll(anyString(), any(Map.class));

        when(template.expire(anyString(), any(Duration.class))).thenReturn(true);
        when(template.expire(anyString(), any(Long.class), any(TimeUnit.class))).thenReturn(true);

        tracker = new RedisReelJobProgressTracker(template);
    }

    @Test
    void writePersistsStageMessagePercentAndSetsTtl() {
        tracker.report("job-1", ReelJobStage.FILTERING, "encode", 42);

        Map<String, String> stored = backing.get("reel:job:job-1");
        assertNotNull(stored);
        assertEquals("FILTERING", stored.get("stage"));
        assertEquals("encode", stored.get("message"));
        assertEquals("42", stored.get("percent"));
        assertNotNull(stored.get("updatedAt"));

        verify(template).expire(eq("reel:job:job-1"), eq(Duration.ofMinutes(15)));
    }

    @Test
    void readReturnsParsedSnapshot() {
        tracker.report("job-2", ReelJobStage.MUSIC, "mixing", 85);

        ReelJobProgressTracker.Snapshot snap = tracker.get("job-2");
        assertNotNull(snap);
        assertEquals(ReelJobStage.MUSIC, snap.stage());
        assertEquals("mixing", snap.message());
        assertEquals(85, snap.percent());
        assertNotNull(snap.updatedAt());
    }

    @Test
    void missingJobReturnsNull() {
        assertNull(tracker.get("nope"));
    }

    @Test
    void blankJobIdIgnored() {
        tracker.report("", ReelJobStage.QUEUED, null, null);
        tracker.report(null, ReelJobStage.QUEUED, null, null);
        assertNull(tracker.get(""));
        assertNull(tracker.get(null));
    }

    @Test
    void percentIsClampedAndMonotonicWithinStage() {
        tracker.report("job-3", ReelJobStage.FILTERING, null, 150);
        assertEquals(100, tracker.get("job-3").percent());

        tracker.report("job-3", ReelJobStage.FILTERING, null, 30);
        assertEquals(100, tracker.get("job-3").percent(),
                "percent must not regress within the same stage");
    }

    @Test
    void percentResetsWhenStageAdvances() {
        tracker.report("job-4", ReelJobStage.FILTERING, null, 70);
        tracker.report("job-4", ReelJobStage.MUSIC, null, 80);

        ReelJobProgressTracker.Snapshot snap = tracker.get("job-4");
        assertEquals(ReelJobStage.MUSIC, snap.stage());
        assertEquals(80, snap.percent());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void reportIsResilientToRedisExceptions() {
        // Make putAll explode; tracker must swallow to avoid breaking the pipeline.
        HashOperations hash = template.opsForHash();
        doAnswer(inv -> { throw new RuntimeException("redis down"); })
                .when(hash).putAll(anyString(), any(Map.class));

        tracker.report("job-5", ReelJobStage.FILTERING, null, 10);
    }
}
