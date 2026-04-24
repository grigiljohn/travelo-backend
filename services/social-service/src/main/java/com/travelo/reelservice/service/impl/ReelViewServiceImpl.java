package com.travelo.reelservice.service.impl;

import com.travelo.reelservice.client.AnalyticsServiceClient;
import com.travelo.reelservice.entity.Reel;
import com.travelo.reelservice.repository.ReelRepository;
import com.travelo.reelservice.service.ReelViewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Default {@link ReelViewService} implementation.
 *
 * <p>The client fires a single "this reel was watched" ping once the video
 * crosses a small playback threshold (usually ~3s or 50% completion). The
 * server then:
 *
 * <ol>
 *   <li>De-duplicates on {@code reelId + userId} via a Redis key with a short
 *       TTL so a user who rewinds / re-watches the same reel within the window
 *       only increments the counter once.</li>
 *   <li>Bumps {@code reels.view_count} atomically via a single JPQL
 *       {@code UPDATE}, avoiding the read-modify-write race that would occur
 *       under the "load entity → setter → save" pattern used by likes.</li>
 *   <li>Fires the analytics event (fire-and-forget; failures are logged but
 *       never propagate to the caller).</li>
 * </ol>
 *
 * Redis is optional: if it is not wired up we simply skip dedupe and still
 * increment the counter. That is safe for the UX because the client is also
 * responsible for not re-firing inside a single viewing session.
 */
@Service
@Transactional(transactionManager = "reelTransactionManager")
public class ReelViewServiceImpl implements ReelViewService {

    private static final Logger logger = LoggerFactory.getLogger(ReelViewServiceImpl.class);

    private static final String DEDUPE_KEY_PREFIX = "reel:view:dedupe:";

    private final ReelRepository reelRepository;
    private final AnalyticsServiceClient analyticsServiceClient;
    private final StringRedisTemplate redisTemplate;

    /** Window during which a repeat view from the same user is deduped. */
    private final Duration dedupeWindow;

    public ReelViewServiceImpl(ReelRepository reelRepository,
                               AnalyticsServiceClient analyticsServiceClient,
                               @Autowired(required = false) StringRedisTemplate redisTemplate,
                               @Value("${reel.view.dedupe-seconds:1800}") long dedupeSeconds) {
        this.reelRepository = reelRepository;
        this.analyticsServiceClient = analyticsServiceClient;
        this.redisTemplate = redisTemplate;
        this.dedupeWindow = Duration.ofSeconds(Math.max(0, dedupeSeconds));
    }

    @Override
    public boolean recordView(UUID reelId,
                              String userId,
                              Integer viewDurationSeconds,
                              Double completionPercentage) {
        if (reelId == null || userId == null || userId.isBlank()) {
            return false;
        }

        // The mobile client carries post.id (the id exposed on the ranked
        // feed surface) as the stable identifier. On this service a Reel is
        // a separate row keyed by its own UUID with a `post_id` FK. Resolve
        // "whatever the client sent" into the real Reel id before touching
        // the counter; this keeps the public API surface forgiving while
        // the write path stays precise.
        UUID resolvedReelId = resolveReelId(reelId);
        if (resolvedReelId == null) {
            logger.debug("No reel matched {} (or its postId form) while recording view", reelId);
            return false;
        }

        if (!shouldRecord(resolvedReelId, userId)) {
            logger.debug("Deduped reel view: reelId={}, userId={}", resolvedReelId, userId);
            return false;
        }

        int updated = reelRepository.incrementViewCount(resolvedReelId);
        if (updated == 0) {
            logger.debug("Reel {} disappeared between lookup and increment", resolvedReelId);
            return false;
        }

        try {
            analyticsServiceClient.trackReelView(
                    resolvedReelId,
                    userId,
                    viewDurationSeconds,
                    normalizeCompletion(completionPercentage)
            );
        } catch (RuntimeException e) {
            // Analytics MUST NOT break the main flow — counter update already
            // committed; just log and move on.
            logger.warn("trackReelView failed for reelId={} userId={}: {}",
                    resolvedReelId, userId, e.getMessage());
        }

        logger.debug("Recorded view: reelId={}, userId={}, duration={}s, completion={}",
                resolvedReelId, userId, viewDurationSeconds, completionPercentage);
        return true;
    }

    /**
     * Resolve a client-supplied id into the real {@link Reel#getId()} UUID.
     *
     * <p>The feed API returns posts (with reel rows attached), so mobile
     * clients often have the post id in hand rather than the reel id. We
     * accept both to keep the view-tracking surface ergonomic.
     */
    private UUID resolveReelId(UUID maybeReelId) {
        if (reelRepository.existsById(maybeReelId)) {
            return maybeReelId;
        }
        Optional<Reel> viaPost = reelRepository.findByPostId(maybeReelId.toString());
        return viaPost.map(Reel::getId).orElse(null);
    }

    /**
     * Try to claim a dedupe slot in Redis via {@code SET ... NX EX}. When
     * Redis is unavailable (either not configured or offline) we fall through
     * to "always record" rather than dropping the event.
     */
    private boolean shouldRecord(UUID reelId, String userId) {
        if (redisTemplate == null || dedupeWindow.isZero() || dedupeWindow.isNegative()) {
            return true;
        }
        try {
            String key = DEDUPE_KEY_PREFIX + reelId + ':' + userId;
            Boolean claimed = redisTemplate.opsForValue()
                    .setIfAbsent(key, "1", dedupeWindow);
            return Boolean.TRUE.equals(claimed);
        } catch (RuntimeException e) {
            logger.debug("Redis dedupe unavailable for reelId={}: {}", reelId, e.getMessage());
            return true;
        }
    }

    /**
     * Clients sometimes send completion as a percentage (0–100) and sometimes
     * as a ratio (0–1). Normalize to ratio for the analytics pipeline.
     */
    private Double normalizeCompletion(Double raw) {
        if (raw == null) {
            return null;
        }
        double v = raw;
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return null;
        }
        if (v > 1.0) {
            v = v / 100.0;
        }
        return Math.max(0.0, Math.min(1.0, v));
    }
}
