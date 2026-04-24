package com.travelo.feedservice.pagination;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedCursorCodecTest {

    private FeedCursorCodec codec;

    @BeforeEach
    void setUp() {
        codec = new FeedCursorCodec("unit-test-secret", 10);
    }

    @Test
    void roundTripPreservesAllFields() {
        FeedCursor original = FeedCursor.of("post-42", 17, "home", "chill",
                Instant.now().toEpochMilli());

        String encoded = codec.encode(original);
        assertNotNull(encoded);
        assertTrue(encoded.contains("."), "encoded cursor must carry its signature");

        FeedCursor roundTrip = codec.decode(encoded);
        assertNotNull(roundTrip);
        assertEquals(original.getLastPostId(), roundTrip.getLastPostId());
        assertEquals(original.getPosition(), roundTrip.getPosition());
        assertEquals(original.getSurface(), roundTrip.getSurface());
        assertEquals(original.getMood(), roundTrip.getMood());
        assertEquals(original.getIssuedAtEpochMillis(), roundTrip.getIssuedAtEpochMillis());
        assertEquals(FeedCursor.CURRENT_VERSION, roundTrip.getVersion());
    }

    @Test
    void tamperedPayloadRejected() {
        FeedCursor original = FeedCursor.of("post-1", 3, "home", null,
                Instant.now().toEpochMilli());
        String encoded = codec.encode(original);
        assertNotNull(encoded);

        int dot = encoded.indexOf('.');
        String payload = encoded.substring(0, dot);
        String sig = encoded.substring(dot + 1);

        // Flip a byte in the payload and keep the original signature; decode must refuse.
        byte[] bytes = Base64.getUrlDecoder().decode(payload);
        bytes[0] ^= 0x01;
        String tamperedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        assertNull(codec.decode(tamperedPayload + "." + sig),
                "tampered payload must fail signature check");
    }

    @Test
    void tamperedSignatureRejected() {
        FeedCursor original = FeedCursor.of("post-1", 3, "home", null,
                Instant.now().toEpochMilli());
        String encoded = codec.encode(original);
        assertNotNull(encoded);

        // Swap the final signature char — must fail verification.
        char last = encoded.charAt(encoded.length() - 1);
        char swapped = last == 'A' ? 'B' : 'A';
        String tampered = encoded.substring(0, encoded.length() - 1) + swapped;

        assertNull(codec.decode(tampered));
    }

    @Test
    void expiredCursorRejected() {
        long pastMillis = Instant.now().minusSeconds(60L * 60L).toEpochMilli(); // 1h ago, ttl is 10m
        FeedCursor stale = FeedCursor.of("post-1", 3, "home", null, pastMillis);
        String encoded = codec.encode(stale);
        assertNotNull(encoded);

        assertNull(codec.decode(encoded), "cursor older than ttl must be rejected");
    }

    @Test
    void differentSecretRejectsCursor() {
        FeedCursor original = FeedCursor.of("post-1", 3, "home", null,
                Instant.now().toEpochMilli());
        String encoded = codec.encode(original);
        assertNotNull(encoded);

        FeedCursorCodec otherCodec = new FeedCursorCodec("different-secret", 10);
        assertNull(otherCodec.decode(encoded),
                "cursor signed with secret A must not verify under secret B");
    }

    @Test
    void legacyPlainPostIdIsAccepted() {
        // Cursors issued before Epic 10 are plain post ids (no dot). They
        // should still decode into a usable FeedCursor so the rollout doesn't
        // invalidate every in-flight mobile session.
        FeedCursor decoded = codec.decode("legacy-post-id-123");
        assertNotNull(decoded);
        assertEquals("legacy-post-id-123", decoded.getLastPostId());
        assertEquals(0, decoded.getPosition());
        assertNull(decoded.getSurface());
    }

    @Test
    void nullAndBlankDecodeToNull() {
        assertNull(codec.decode(null));
        assertNull(codec.decode(""));
        assertNull(codec.decode("   "));
    }

    @Test
    void contextMismatchDetected() {
        FeedCursor issued = FeedCursor.of("p", 1, "home", "chill",
                Instant.now().toEpochMilli());
        // Same context → ok.
        assertTrue(issued.matchesContext("home", "chill"));
        // Case-insensitive surface match.
        assertTrue(issued.matchesContext("HOME", "chill"));
        // Different mood → drift.
        assertFalse(issued.matchesContext("home", "adventure"));
        // Different surface → drift.
        assertFalse(issued.matchesContext("explore", "chill"));
    }

    @Test
    void contextMatchesWhenBothSidesBlankMood() {
        FeedCursor issued = FeedCursor.of("p", 1, "home", null,
                Instant.now().toEpochMilli());
        assertTrue(issued.matchesContext("home", null));
        assertTrue(issued.matchesContext("home", ""));
    }
}
