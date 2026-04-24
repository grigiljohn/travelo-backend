package com.travelo.mediaservice.reel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pure JSON round-trip tests for the wire format used by
 * {@link RedisReelJobProgressBroker} Pub/Sub messages. Avoids touching any
 * Redis infrastructure — we only care that the codec stays stable.
 */
class RedisReelJobProgressBrokerWireTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void fromWireRoundTripsAllFields() throws Exception {
        String json = "{\"stage\":\"FILTERING\",\"percent\":42,\"message\":\"encode\",\"updatedAt\":\"2026-04-20T10:00:00Z\"}";
        @SuppressWarnings("rawtypes")
        Map raw = om.readValue(json, Map.class);
        ReelJobProgressTracker.Snapshot s = RedisReelJobProgressBroker.fromWire(raw);

        assertNotNull(s);
        assertEquals(ReelJobStage.FILTERING, s.stage());
        assertEquals(42, s.percent());
        assertEquals("encode", s.message());
        assertEquals(Instant.parse("2026-04-20T10:00:00Z"), s.updatedAt());
    }

    @Test
    void fromWireTolerantToMissingOptionalFields() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stage", "READY");
        m.put("updatedAt", "2026-04-20T10:00:00Z");
        ReelJobProgressTracker.Snapshot s = RedisReelJobProgressBroker.fromWire(m);

        assertNotNull(s);
        assertEquals(ReelJobStage.READY, s.stage());
        assertNull(s.percent());
        assertNull(s.message());
    }

    @Test
    void fromWireReturnsNullForUnknownStageOrMissingStage() {
        Map<String, Object> bad = new LinkedHashMap<>();
        bad.put("stage", "DOES_NOT_EXIST");
        assertNull(RedisReelJobProgressBroker.fromWire(bad));

        Map<String, Object> empty = new LinkedHashMap<>();
        assertNull(RedisReelJobProgressBroker.fromWire(empty));
    }

    @Test
    void fromWireAcceptsStringifiedPercent() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stage", "MUSIC");
        m.put("percent", "77");
        m.put("updatedAt", "2026-04-20T10:00:00Z");
        ReelJobProgressTracker.Snapshot s = RedisReelJobProgressBroker.fromWire(m);

        assertEquals(77, s.percent());
    }
}
