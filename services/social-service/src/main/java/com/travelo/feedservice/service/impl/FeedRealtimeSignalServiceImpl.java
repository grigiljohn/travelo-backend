package com.travelo.feedservice.service.impl;

import com.travelo.feedservice.client.dto.PostDto;
import com.travelo.feedservice.dto.FeedItem;
import com.travelo.feedservice.dto.FeedUserEventDto;
import com.travelo.feedservice.service.FeedMetricsService;
import com.travelo.feedservice.service.FeedRealtimeSignalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class FeedRealtimeSignalServiceImpl implements FeedRealtimeSignalService {

    private static final Logger logger = LoggerFactory.getLogger(FeedRealtimeSignalServiceImpl.class);
    private static final String SIGNAL_KEY_PREFIX = "feed:signal:";
    private static final String AUTHOR_WINDOW_KEY_PREFIX = "feed:author-window:";

    private final RedisTemplate<String, String> redisTemplate;
    private final HashOperations<String, String, String> hashOps;
    private final boolean enabled;
    private final int signalTtlMinutes;
    private final int authorWindowTtlMinutes;
    private final double onlineSignalWeight;
    private final double authorFatiguePenaltyPerExposure;
    private final FeedMetricsService feedMetricsService;

    public FeedRealtimeSignalServiceImpl(
            @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate,
            @Value("${app.feed.online-signals.enabled:true}") boolean enabled,
            @Value("${app.feed.online-signals.signal-ttl-minutes:180}") int signalTtlMinutes,
            @Value("${app.feed.online-signals.author-window-ttl-minutes:45}") int authorWindowTtlMinutes,
            @Value("${app.feed.online-signals.weight:0.25}") double onlineSignalWeight,
            @Value("${app.feed.online-signals.author-fatigue-penalty-per-exposure:0.12}") double authorFatiguePenaltyPerExposure,
            FeedMetricsService feedMetricsService) {
        this.redisTemplate = redisTemplate;
        this.hashOps = redisTemplate.opsForHash();
        this.enabled = enabled;
        this.signalTtlMinutes = Math.max(5, signalTtlMinutes);
        this.authorWindowTtlMinutes = Math.max(5, authorWindowTtlMinutes);
        this.onlineSignalWeight = Math.max(0.0, Math.min(1.0, onlineSignalWeight));
        this.authorFatiguePenaltyPerExposure = Math.max(0.0, Math.min(0.8, authorFatiguePenaltyPerExposure));
        this.feedMetricsService = feedMetricsService;
    }

    @Override
    public void recordUserEvents(UUID userId, String surface, List<FeedUserEventDto> events) {
        if (!enabled || events == null || events.isEmpty()) {
            return;
        }
        String signalKey = signalKey(userId, surface);
        try {
            for (FeedUserEventDto e : events) {
                if (e == null || e.getTargetId() == null || e.getTargetId().isBlank()) {
                    continue;
                }
                String targetId = e.getTargetId().trim();
                double delta = eventDelta(e.getEventType(), e.getDwellMs());
                if (delta == 0.0d) {
                    continue;
                }
                feedMetricsService.recordOnlineSignalEvent(e.getEventType());
                double current = parseDoubleOrZero(hashOps.get(signalKey, targetId));
                double next = clampSignal(current + delta);
                hashOps.put(signalKey, targetId, Double.toString(next));
            }
            redisTemplate.expire(signalKey, signalTtlMinutes, TimeUnit.MINUTES);
        } catch (Exception ex) {
            logger.warn("feed online signal recording failed user={} surface={} err={}", userId, surface, ex.toString());
        }
    }

    @Override
    public List<PostDto> applyOnlineSignals(UUID userId, String surface, List<PostDto> rankedPosts) {
        if (!enabled || rankedPosts == null || rankedPosts.isEmpty()) {
            return rankedPosts;
        }
        String signalKey = signalKey(userId, surface);
        try {
            List<ScoredPost> scored = new ArrayList<>(rankedPosts.size());
            for (int i = 0; i < rankedPosts.size(); i++) {
                PostDto p = rankedPosts.get(i);
                String postId = p != null ? p.getId() : null;
                double base = 1.0d - ((double) i / Math.max(1.0, rankedPosts.size()));
                double signal = postId == null ? 0.0d : parseDoubleOrZero(hashOps.get(signalKey, postId));
                double finalScore = base + (signal * onlineSignalWeight);
                scored.add(new ScoredPost(i, p, finalScore));
            }
            return scored.stream()
                    .sorted((a, b) -> {
                        int cmp = Double.compare(b.score, a.score);
                        if (cmp != 0) {
                            return cmp;
                        }
                        return Integer.compare(a.originalIndex, b.originalIndex);
                    })
                    .map(s -> s.post)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            logger.warn("feed online signal rerank failed user={} surface={} err={}", userId, surface, ex.toString());
            return rankedPosts;
        }
    }

    @Override
    public double getOnlineSignalScore(UUID userId, String surface, String targetId) {
        if (!enabled || targetId == null || targetId.isBlank()) {
            return 0.0d;
        }
        try {
            String v = hashOps.get(signalKey(userId, surface), targetId);
            return parseDoubleOrZero(v);
        } catch (Exception ex) {
            return 0.0d;
        }
    }

    @Override
    public List<FeedItem> applySessionFatigue(UUID userId, String surface, List<FeedItem> feedItems) {
        if (!enabled || feedItems == null || feedItems.isEmpty()) {
            return feedItems;
        }
        String authorKey = authorWindowKey(userId, surface);
        try {
            List<Integer> contentSlots = new ArrayList<>();
            List<FeedItem> contentItems = new ArrayList<>();
            for (int i = 0; i < feedItems.size(); i++) {
                FeedItem item = feedItems.get(i);
                if (item == null) {
                    continue;
                }
                if ("post".equals(item.getType()) || "reel".equals(item.getType())) {
                    contentSlots.add(i);
                    contentItems.add(item);
                }
            }
            if (contentItems.size() <= 1) {
                return feedItems;
            }

            List<ScoredFeedItem> reranked = new ArrayList<>(contentItems.size());
            for (int i = 0; i < contentItems.size(); i++) {
                FeedItem item = contentItems.get(i);
                String authorId = (item.getPost() != null) ? item.getPost().getUserId() : null;
                double recentCount = authorId == null ? 0.0d : parseDoubleOrZero(hashOps.get(authorKey, authorId));
                double score = (1.0d - ((double) i / Math.max(1.0, contentItems.size())))
                        - (recentCount * authorFatiguePenaltyPerExposure);
                reranked.add(new ScoredFeedItem(i, item, score));
            }

            List<FeedItem> output = new ArrayList<>(feedItems);
            List<FeedItem> rerankedContent = reranked.stream()
                    .sorted((a, b) -> {
                        int cmp = Double.compare(b.score, a.score);
                        if (cmp != 0) {
                            return cmp;
                        }
                        return Integer.compare(a.originalIndex, b.originalIndex);
                    })
                    .map(s -> s.item)
                    .toList();

            for (int i = 0; i < contentSlots.size(); i++) {
                output.set(contentSlots.get(i), rerankedContent.get(i));
            }
            int reordered = 0;
            for (int i = 0; i < contentItems.size(); i++) {
                if (contentItems.get(i) != rerankedContent.get(i)) {
                    reordered++;
                }
            }
            if (reordered > 0) {
                feedMetricsService.recordFatigueReorder(surface, reordered);
            }
            return output;
        } catch (Exception ex) {
            logger.warn("feed fatigue rerank failed user={} surface={} err={}", userId, surface, ex.toString());
            return feedItems;
        }
    }

    @Override
    public void recordServedItems(UUID userId, String surface, List<FeedItem> servedItems) {
        if (!enabled || servedItems == null || servedItems.isEmpty()) {
            return;
        }
        String authorKey = authorWindowKey(userId, surface);
        try {
            for (FeedItem item : servedItems) {
                if (item == null || item.getPost() == null) {
                    continue;
                }
                if (!"post".equals(item.getType()) && !"reel".equals(item.getType())) {
                    continue;
                }
                String authorId = item.getPost().getUserId();
                if (authorId == null || authorId.isBlank()) {
                    continue;
                }
                double current = parseDoubleOrZero(hashOps.get(authorKey, authorId));
                hashOps.put(authorKey, authorId, Double.toString(current + 1.0d));
            }
            redisTemplate.expire(authorKey, authorWindowTtlMinutes, TimeUnit.MINUTES);
        } catch (Exception ex) {
            logger.warn("feed served-item tracking failed user={} surface={} err={}", userId, surface, ex.toString());
        }
    }

    private String signalKey(UUID userId, String surface) {
        return SIGNAL_KEY_PREFIX + userId + ":" + normalizeSurface(surface);
    }

    private String authorWindowKey(UUID userId, String surface) {
        return AUTHOR_WINDOW_KEY_PREFIX + userId + ":" + normalizeSurface(surface);
    }

    private String normalizeSurface(String surface) {
        if (surface == null || surface.isBlank()) {
            return "home";
        }
        return surface.trim().toLowerCase(Locale.ROOT);
    }

    private double parseDoubleOrZero(String value) {
        if (value == null || value.isBlank()) {
            return 0.0d;
        }
        try {
            return Double.parseDouble(value);
        } catch (Exception ignore) {
            return 0.0d;
        }
    }

    private double eventDelta(String eventType, Integer dwellMs) {
        if (eventType == null || eventType.isBlank()) {
            return 0.0d;
        }
        String type = eventType.trim().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "impression" -> 0.05d + dwellDelta(dwellMs);
            case "click", "open", "like", "save", "share", "comment" -> 0.35d + dwellDelta(dwellMs);
            case "hide", "not_interested", "report" -> -0.6d;
            default -> dwellDelta(dwellMs);
        };
    }

    private double dwellDelta(Integer dwellMs) {
        if (dwellMs == null || dwellMs <= 0) {
            return 0.0d;
        }
        if (dwellMs >= 12000) {
            return 0.25d;
        }
        if (dwellMs >= 5000) {
            return 0.12d;
        }
        if (dwellMs >= 1500) {
            return 0.05d;
        }
        return 0.0d;
    }

    private double clampSignal(double v) {
        return Math.max(-1.0d, Math.min(2.0d, v));
    }

    private record ScoredPost(int originalIndex, PostDto post, double score) {}

    private record ScoredFeedItem(int originalIndex, FeedItem item, double score) {}
}

