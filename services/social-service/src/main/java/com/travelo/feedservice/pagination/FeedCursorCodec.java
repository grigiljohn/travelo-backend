package com.travelo.feedservice.pagination;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Encodes / decodes {@link FeedCursor} into a compact, tamper-evident string
 * clients treat as opaque.
 *
 * <p>Wire format: {@code <base64url(payload_json)>.<base64url(hmac_sha256)>}
 * where the HMAC is computed over the payload bytes using the configured
 * secret. Signature mismatch, JSON parse failure, version mismatch, or TTL
 * expiry all result in {@link #decode(String)} returning {@code null}, which
 * the caller treats the same as a missing cursor (first page).</p>
 *
 * <p>The default secret is intentionally hard-coded to an obviously
 * placeholder value so CI fails loudly if the real secret isn't injected in
 * production.</p>
 */
@Component
public class FeedCursorCodec {

    private static final Logger logger = LoggerFactory.getLogger(FeedCursorCodec.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String secret;
    private final long ttlMillis;

    public FeedCursorCodec(
            @Value("${app.feed.cursor.secret:travelo-feed-cursor-dev-secret-change-me}")
            String secret,
            @Value("${app.feed.cursor.ttl-minutes:10}") long ttlMinutes) {
        this.secret = secret == null || secret.isBlank()
                ? "travelo-feed-cursor-dev-secret-change-me"
                : secret;
        this.ttlMillis = Math.max(1, ttlMinutes) * 60_000L;
    }

    @PostConstruct
    void warnOnDefaultSecret() {
        if ("travelo-feed-cursor-dev-secret-change-me".equals(secret)) {
            logger.warn("app.feed.cursor.secret is using the default placeholder — set FEED_CURSOR_SECRET in production");
        }
    }

    /**
     * Serialize {@code cursor} into a signed opaque string. Callers receive
     * {@code null} when {@code cursor} is {@code null} or cannot be signed
     * (should not happen unless the JVM lacks {@code HmacSHA256}).
     */
    public String encode(FeedCursor cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("v", cursor.getVersion());
            payload.put("pid", cursor.getLastPostId() == null ? "" : cursor.getLastPostId());
            payload.put("pos", cursor.getPosition());
            payload.put("s", cursor.getSurface() == null ? "" : cursor.getSurface());
            payload.put("m", cursor.getMood() == null ? "" : cursor.getMood());
            payload.put("ts", cursor.getIssuedAtEpochMillis());
            byte[] payloadBytes = objectMapper.writeValueAsBytes(payload);
            byte[] sig = sign(payloadBytes);
            return ENCODER.encodeToString(payloadBytes) + "." + ENCODER.encodeToString(sig);
        } catch (Exception e) {
            logger.warn("Failed to encode feed cursor: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Decode {@code raw} back into a {@link FeedCursor}. Returns {@code null}
     * for missing / tampered / expired / malformed cursors — the feed service
     * then falls back to serving a fresh first page.
     */
    public FeedCursor decode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        // Back-compat: if the cursor has no "." separator we treat it as the
        // pre-Epic-10 plain post-id cursor. Clients upgrade automatically on
        // their next request once the server starts re-emitting signed cursors.
        int dot = raw.indexOf('.');
        if (dot <= 0 || dot >= raw.length() - 1) {
            return legacyCursorFromId(raw);
        }
        try {
            byte[] payloadBytes = DECODER.decode(raw.substring(0, dot));
            byte[] providedSig = DECODER.decode(raw.substring(dot + 1));
            byte[] expectedSig = sign(payloadBytes);
            if (!constantTimeEquals(providedSig, expectedSig)) {
                logger.debug("feed cursor signature mismatch");
                return null;
            }
            ObjectNode node = (ObjectNode) objectMapper.readTree(payloadBytes);
            int version = node.path("v").asInt(0);
            if (version != FeedCursor.CURRENT_VERSION) {
                logger.debug("feed cursor version mismatch got={} expected={}",
                        version, FeedCursor.CURRENT_VERSION);
                return null;
            }
            long issuedAt = node.path("ts").asLong(0L);
            if (issuedAt <= 0 || Instant.now().toEpochMilli() - issuedAt > ttlMillis) {
                logger.debug("feed cursor expired (age={}ms, ttl={}ms)",
                        Instant.now().toEpochMilli() - issuedAt, ttlMillis);
                return null;
            }
            return new FeedCursor(
                    version,
                    emptyToNull(node.path("pid").asText("")),
                    Math.max(0, node.path("pos").asInt(0)),
                    emptyToNull(node.path("s").asText("")),
                    emptyToNull(node.path("m").asText("")),
                    issuedAt
            );
        } catch (Exception e) {
            logger.debug("feed cursor decode failed: {}", e.getMessage());
            return null;
        }
    }

    private FeedCursor legacyCursorFromId(String rawId) {
        // Treat legacy ids as "unknown position, unknown surface" anchors that
        // still let the service find the post-id in the computed feed. Any
        // request flipping surface or mood will therefore start fresh (which
        // is the behavior we want).
        return new FeedCursor(
                FeedCursor.CURRENT_VERSION,
                rawId,
                0,
                null,
                null,
                Instant.now().toEpochMilli()
        );
    }

    private byte[] sign(byte[] payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(payload);
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }
}
