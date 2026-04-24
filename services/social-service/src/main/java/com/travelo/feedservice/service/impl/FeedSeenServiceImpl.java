package com.travelo.feedservice.service.impl;

import com.travelo.feedservice.service.FeedSeenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis ZSET-based implementation of seen-post tracking.
 *
 * <p>Key format: {@code seen:ts:{userId}:{surface}}<br>
 * Score:       epoch seconds (first-seen-at)<br>
 * Member:      post id string
 *
 * <p>The ZSET is trimmed opportunistically on each write: everything older
 * than the retention TTL is removed via {@code ZREMRANGEBYSCORE}. We still
 * call {@code EXPIRE} as a safety net so abandoned keys disappear.
 */
@Service
public class FeedSeenServiceImpl implements FeedSeenService {

    private static final Logger logger = LoggerFactory.getLogger(FeedSeenServiceImpl.class);

    /** New ZSET namespace; distinct prefix so a hot cluster migrating from the
     *  legacy SET layout doesn't trip over mixed types on the same key. */
    private static final String SEEN_KEY_PREFIX = "seen:ts:";

    private static final int DEFAULT_TTL_DAYS = 7;
    private static final int MAX_TTL_DAYS = 30;
    private static final int DEFAULT_HARD_FILTER_WINDOW_HOURS = 24;

    private final RedisTemplate<String, String> redisTemplate;
    private final ZSetOperations<String, String> zsetOps;
    private final int ttlDays;
    private final long ttlSeconds;
    private final long hardFilterWindowSeconds;
    private final boolean enabled;

    public FeedSeenServiceImpl(
            @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate,
            @Value("${app.feed.seen-ttl-days:" + DEFAULT_TTL_DAYS + "}") int ttlDays,
            @Value("${app.feed.seen.hard-filter-window-hours:" + DEFAULT_HARD_FILTER_WINDOW_HOURS + "}") int hardFilterWindowHours,
            @Value("${app.feed.seen-enabled:true}") boolean enabled) {
        this.redisTemplate = redisTemplate;
        this.zsetOps = redisTemplate.opsForZSet();
        this.ttlDays = Math.min(Math.max(ttlDays, 1), MAX_TTL_DAYS);
        this.ttlSeconds = Duration.ofDays(this.ttlDays).getSeconds();
        int clampedHardHours = Math.max(0, Math.min(hardFilterWindowHours, this.ttlDays * 24));
        this.hardFilterWindowSeconds = Duration.ofHours(clampedHardHours).getSeconds();
        this.enabled = enabled;
        logger.info("FeedSeenService initialized: enabled={}, ttlDays={}, hardFilterWindowHours={}",
                enabled, this.ttlDays, clampedHardHours);
    }

    @Override
    public void markPostsAsSeen(UUID userId, String surface, Set<String> postIds) {
        if (!enabled || postIds == null || postIds.isEmpty()) {
            return;
        }
        String key = getSeenKey(userId, surface);
        long now = Instant.now().getEpochSecond();
        try {
            Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>(postIds.size());
            for (String postId : postIds) {
                if (postId == null || postId.isBlank()) continue;
                tuples.add(ZSetOperations.TypedTuple.of(postId, (double) now));
            }
            if (tuples.isEmpty()) return;

            // addIfAbsent preserves the first-seen timestamp on repeat exposures —
            // essential for the hard-filter window to stay accurate.
            zsetOps.addIfAbsent(key, tuples);
            // Evict anything older than the retention horizon so the ZSET doesn't grow unbounded.
            zsetOps.removeRangeByScore(key, 0, (double) (now - ttlSeconds));
            redisTemplate.expire(key, ttlDays, TimeUnit.DAYS);
        } catch (Exception e) {
            logger.warn("flow=seen_mark_failed userId={} surface={} err={}", userId, surface, e.toString());
        }
    }

    @Override
    public Map<String, SeenState> classifySeen(UUID userId, String surface, Set<String> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, SeenState> result = new HashMap<>(postIds.size());
        if (!enabled) {
            for (String id : postIds) result.put(id, SeenState.UNSEEN);
            return result;
        }

        String key = getSeenKey(userId, surface);
        long now = Instant.now().getEpochSecond();
        long hardCutoff = now - hardFilterWindowSeconds;

        try {
            for (String postId : postIds) {
                if (postId == null || postId.isBlank()) continue;
                Double score = zsetOps.score(key, postId);
                if (score == null) {
                    result.put(postId, SeenState.UNSEEN);
                } else if (score.longValue() >= hardCutoff) {
                    result.put(postId, SeenState.HARD);
                } else {
                    result.put(postId, SeenState.SOFT);
                }
            }
        } catch (Exception e) {
            logger.warn("flow=seen_classify_failed userId={} surface={} err={}. Treating all as unseen.",
                    userId, surface, e.toString());
            for (String id : postIds) result.put(id, SeenState.UNSEEN);
        }
        return result;
    }

    @Override
    public Set<String> getSeenPostIds(UUID userId, String surface, Set<String> postIds) {
        if (!enabled || postIds == null || postIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<String> out = new HashSet<>();
        Map<String, SeenState> classified = classifySeen(userId, surface, postIds);
        for (Map.Entry<String, SeenState> e : classified.entrySet()) {
            if (e.getValue() != SeenState.UNSEEN) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    @Override
    public boolean isPostSeen(UUID userId, String surface, String postId) {
        if (!enabled || postId == null || postId.isBlank()) {
            return false;
        }
        try {
            Double score = zsetOps.score(getSeenKey(userId, surface), postId);
            return score != null;
        } catch (Exception e) {
            logger.debug("flow=seen_lookup_failed userId={} surface={} postId={} err={}",
                    userId, surface, postId, e.toString());
            return false;
        }
    }

    @Override
    public void clearSeenPosts(UUID userId, String surface) {
        String key = getSeenKey(userId, surface);
        try {
            redisTemplate.delete(key);
            logger.info("flow=seen_cleared userId={} surface={}", userId, surface);
        } catch (Exception e) {
            logger.warn("flow=seen_clear_failed userId={} surface={} err={}", userId, surface, e.toString());
        }
    }

    private String getSeenKey(UUID userId, String surface) {
        return SEEN_KEY_PREFIX + userId + ":" + normalizeSurface(surface);
    }

    private static String normalizeSurface(String surface) {
        if (surface == null || surface.isBlank()) {
            return "home";
        }
        return surface.trim().toLowerCase();
    }

}
