package com.travelo.feedservice.service.impl;

import com.travelo.feedservice.client.AdServiceClient;
import com.travelo.feedservice.client.PostServiceClient;
import com.travelo.feedservice.client.StoryServiceClient;
import com.travelo.feedservice.client.UserServiceClient;
import com.travelo.feedservice.client.dto.AdDeliveryResponse;
import com.travelo.feedservice.client.dto.PostDto;
import com.travelo.feedservice.client.dto.StoryPreviewDto;
import com.travelo.feedservice.dto.FeedItem;
import com.travelo.feedservice.dto.FeedRankingDebugItem;
import com.travelo.feedservice.dto.FeedRankingDebugResponse;
import com.travelo.feedservice.dto.FeedResponse;
import com.travelo.feedservice.pagination.FeedCursor;
import com.travelo.feedservice.pagination.FeedCursorCodec;
import com.travelo.feedservice.service.FeedCacheService;
import com.travelo.feedservice.service.FeedMetricsService;
import com.travelo.feedservice.service.FeedRealtimeSignalService;
import com.travelo.feedservice.service.FeedRankingService;
import com.travelo.feedservice.service.FeedSeenService;
import com.travelo.feedservice.service.FeedSeenService.SeenState;
import com.travelo.feedservice.service.FeedService;
import com.travelo.planservice.dto.PlanResponse;
import com.travelo.planservice.service.PlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Enhanced feed service implementation with ranking, caching, and fan-out support.
 */
@Service
public class FeedServiceImpl implements FeedService {

    private static final Logger logger = LoggerFactory.getLogger(FeedServiceImpl.class);

    private final PostServiceClient postServiceClient;
    private final AdServiceClient adServiceClient;
    private final StoryServiceClient storyServiceClient;
    private final UserServiceClient userServiceClient;
    private final FeedRankingService feedRankingService;
    private final FeedRealtimeSignalService feedRealtimeSignalService;
    private final FeedMetricsService feedMetricsService;
    private final FeedCacheService feedCacheService;
    private final FeedSeenService feedSeenService;
    private final FeedCursorCodec feedCursorCodec;
    private final PlanService planService;

    private final boolean cacheEnabled;
    private final boolean fanOutEnabled;
    private final double seenSoftPenalty;

    // Ad insertion configuration
    private static final int MIN_POSTS_BETWEEN_ADS = 5;
    private static final int MAX_POSTS_BETWEEN_ADS = 7;
    
    // Feed computation limits
    private static final int MAX_FEED_COMPUTATION_LIMIT = 500;
    private static final int MAX_STORY_PREVIEWS_IN_FEED = 24;

    /** Insert a circle plan after every Nth post/reel on the home surface. */
    private static final int HOME_FEED_PLAN_EVERY_N_CONTENT = 5;

    private final int recommendedPrefetchLimit;

    public FeedServiceImpl(
            PostServiceClient postServiceClient,
            AdServiceClient adServiceClient,
            StoryServiceClient storyServiceClient,
            UserServiceClient userServiceClient,
            FeedRankingService feedRankingService,
            FeedRealtimeSignalService feedRealtimeSignalService,
            FeedMetricsService feedMetricsService,
            FeedCacheService feedCacheService,
            FeedSeenService feedSeenService,
            FeedCursorCodec feedCursorCodec,
            PlanService planService,
            @Value("${app.feed.cache-enabled:true}") boolean cacheEnabled,
            @Value("${app.feed.fan-out-enabled:true}") boolean fanOutEnabled,
            @Value("${app.feed.recommended-prefetch-limit:12}") int recommendedPrefetchLimit,
            @Value("${app.feed.seen.soft-penalty:0.35}") double seenSoftPenalty) {
        this.postServiceClient = postServiceClient;
        this.adServiceClient = adServiceClient;
        this.storyServiceClient = storyServiceClient;
        this.userServiceClient = userServiceClient;
        this.feedRankingService = feedRankingService;
        this.feedRealtimeSignalService = feedRealtimeSignalService;
        this.feedMetricsService = feedMetricsService;
        this.feedCacheService = feedCacheService;
        this.feedSeenService = feedSeenService;
        this.feedCursorCodec = feedCursorCodec;
        this.planService = planService;
        this.cacheEnabled = cacheEnabled;
        this.fanOutEnabled = fanOutEnabled;
        this.recommendedPrefetchLimit = recommendedPrefetchLimit;
        // Clamp to [0, 1]: 0 => no demotion (equivalent to hard-filter-only),
        // 1 => maximum demotion (equivalent to hard-filter everything seen).
        this.seenSoftPenalty = Math.max(0.0d, Math.min(1.0d, seenSoftPenalty));
    }

    @Override
    public FeedResponse getFeed(UUID userId, String cursor, int limit, String mood, String surface) {
        return feedMetricsService.timeGetFeed(surface, () -> {
            logger.info("Getting feed for userId={}, cursor=<{} chars>, limit={}, mood={}, surface={}",
                    userId, cursor == null ? 0 : cursor.length(), limit, mood, surface);

            // Decode the opaque cursor. A null / stale / tampered cursor is
            // treated identically to a first-page request so the caller sees
            // a fresh ranked slice instead of a silent failure.
            FeedCursor decodedCursor = feedCursorCodec.decode(cursor);
            if (decodedCursor != null && !decodedCursor.matchesContext(surface, mood)) {
                // Filter context shifted mid-session (e.g. user changed mood).
                // Serving the old cursor would let us slice into a feed built
                // with a different filter; start fresh instead.
                logger.debug("flow=feed_cursor_context_reset surface={} mood={} cursorSurface={} cursorMood={}",
                        surface, mood, decodedCursor.getSurface(), decodedCursor.getMood());
                decodedCursor = null;
            }

            // Account for seen filtering and cursor depth: a 5-pages-in request
            // still needs all earlier items in memory so we can locate the
            // cursor anchor (or its position fallback).
            int position = decodedCursor != null ? decodedCursor.getPosition() : 0;
            int baseFetch = Math.max(limit * 3, limit + 16);
            int fetchLimit = Math.min(
                    Math.max(baseFetch, position + limit * 2 + 16),
                    MAX_FEED_COMPUTATION_LIMIT);

            List<FeedItem> feedItems;
            boolean fromNonEmptyCache = false;
            if (cacheEnabled && feedCacheService.isFeedCached(userId)) {
                List<FeedItem> cachedItems = feedCacheService.getCachedFeed(userId, null, fetchLimit);
                if (!cachedItems.isEmpty()) {
                    logger.debug("Retrieved {} items from cache (fetchLimit={})", cachedItems.size(), fetchLimit);
                    feedItems = cachedItems;
                    fromNonEmptyCache = true;
                } else {
                    feedItems = computeFeed(userId, fetchLimit, mood, surface);
                }
            } else {
                feedItems = computeFeed(userId, fetchLimit, mood, surface);
            }

            // Home: (re)inject circle plans after cache OR fresh compute. Strip existing plan rows first
            // so every-N cadence stays idempotent when Redis returns an older layout.
            if ("home".equalsIgnoreCase(surface)) {
                feedItems = injectHomePlans(feedItems, surface, fetchLimit);
            }
            if (cacheEnabled && !feedItems.isEmpty()) {
                if ("home".equalsIgnoreCase(surface)) {
                    feedCacheService.cacheFeed(userId, feedItems);
                } else if (!fromNonEmptyCache) {
                    feedCacheService.cacheFeed(userId, feedItems);
                    logger.debug("Cached {} feed items for user {}", feedItems.size(), userId);
                }
            }

            // CRITICAL: filter seen posts BEFORE cursor pagination. Cursor
            // resolution below operates on the post-filter list so we don't
            // re-serve items that became seen between requests.
            feedItems = suppressSeenPosts(userId, surface, feedItems);
            feedItems = feedRealtimeSignalService.applySessionFatigue(userId, surface, feedItems);

            CursorSlice slice = applyCursorPagination(feedItems, decodedCursor, limit);
            feedRealtimeSignalService.recordServedItems(userId, surface, slice.items);

            long contentCount = slice.items.stream()
                    .filter(item -> "post".equals(item.getType()) || "reel".equals(item.getType()))
                    .count();
            long adCount = slice.items.stream()
                    .filter(item -> "ad".equals(item.getType()))
                    .count();
            feedMetricsService.recordFeedServed(
                    surface,
                    slice.items.size(),
                    (int) contentCount,
                    (int) adCount
            );
            return buildFeedResponse(slice, limit, surface, mood);
        });
    }

    @Override
    public FeedRankingDebugResponse debugRanking(UUID userId, int limit, String mood, String surface) {
        int fetchLimit = Math.min(Math.max(limit, 1) * 3, MAX_FEED_COMPUTATION_LIMIT);
        List<UUID> followedUserIds = userServiceClient.getFollowing(userId);
        Set<String> followedSet = followedUserIds.stream().map(UUID::toString).collect(Collectors.toSet());
        List<PostDto> posts = fetchPosts(fetchLimit, mood, followedUserIds, userId.toString());
        List<PostDto> ranked = feedRankingService.rankPosts(posts, userId, followedUserIds);

        List<PostDto> rankedLimited = ranked.stream()
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
        Set<String> postIdsForSeenLookup = rankedLimited.stream()
                .filter(Objects::nonNull)
                .map(PostDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, SeenState> seenClassification = postIdsForSeenLookup.isEmpty()
                ? Collections.emptyMap()
                : feedSeenService.classifySeen(userId, surface, postIdsForSeenLookup);

        List<FeedRankingDebugItem> debugItems = rankedLimited.stream()
                .map(p -> {
                    boolean following = p != null && followedSet.contains(p.getUserId());
                    FeedRankingDebugItem item = feedRankingService.buildDebugItem(p, userId, following);
                    double onlineSignal = feedRealtimeSignalService.getOnlineSignalScore(userId, surface, p.getId());
                    item.setOnlineSignalScore(onlineSignal);
                    item.setFinalScore(item.getBaseScore() + onlineSignal * 0.25d);
                    SeenState state = p != null && p.getId() != null
                            ? seenClassification.getOrDefault(p.getId(), SeenState.UNSEEN)
                            : SeenState.UNSEEN;
                    item.setSeenState(state.name().toLowerCase());
                    return item;
                })
                .collect(Collectors.toList());

        FeedRankingDebugResponse response = new FeedRankingDebugResponse();
        response.setSurface(surface);
        response.setMood(mood);
        response.setRequestedLimit(limit);
        response.setItems(debugItems);
        response.setConfig(Map.of(
                "note", "final_score includes online signal boost with configured weight",
                "item_count", debugItems.size()
        ));
        return response;
    }

    @Override
    public void refreshFeed(UUID userId) {
        logger.info("Refreshing feed for userId={}", userId);
        
        // Invalidate cache
        if (cacheEnabled) {
            feedCacheService.invalidateFeed(userId);
        }
        
        // Trigger recomputation (will happen on next getFeed call)
        logger.info("Feed cache invalidated for user {}", userId);
    }

    @Override
    public void addPostToFeed(UUID userId, UUID postId) {
        if (!fanOutEnabled) {
            logger.debug("Fan-out disabled, skipping feed update for user {}", userId);
            return;
        }

        logger.debug("Adding post {} to feed for user {} (fan-out)", postId, userId);
        
        // Invalidate cache to force recomputation
        if (cacheEnabled) {
            feedCacheService.invalidateFeed(userId);
        }
        
        // In a full implementation, we could add the post directly to the cache
        // For now, we invalidate and let it recompute on next read
    }

    /**
     * Compute personalized feed from scratch.
     */
    private List<FeedItem> computeFeed(UUID userId, int limit, String mood, String surface) {
        logger.debug("Computing feed for user {} with limit {}", userId, limit);

        // Step 1: Get following list
        List<UUID> followedUserIds = userServiceClient.getFollowing(userId);
        logger.debug("User {} follows {} users", userId, followedUserIds.size());

        // Step 2: Fetch posts (from followed users + recommendations)
        final String viewerId = userId.toString();
        List<PostDto> posts = fetchPosts(limit, mood, followedUserIds, viewerId);
        logger.debug("Fetched {} posts", posts.size());

        if (posts.isEmpty()) {
            if (!"home".equalsIgnoreCase(surface)) {
                return Collections.emptyList();
            }
            List<PlanResponse> plansOnly = planService.listRecentPlansForFeed(Math.min(40, limit));
            if (plansOnly.isEmpty()) {
                return Collections.emptyList();
            }
            List<FeedItem> planFeed = plansOnly.stream().map(FeedItem::fromPlan).collect(Collectors.toList());
            CompletableFuture<List<StoryPreviewDto>> storiesOnly = CompletableFuture.supplyAsync(
                    () -> storyServiceClient.fetchDiscoverStories(viewerId)
            );
            planFeed = injectStoryCluster(planFeed, storiesOnly.join());
            logger.info("Computed plan-only home feed with {} items", planFeed.size());
            return planFeed;
        }

        // Step 3/4/5 in parallel: rank posts + fetch ads + discover stories (fail-open for ads/stories).
        int adCount = calculateAdCount(posts.size());
        CompletableFuture<List<PostDto>> rankedFuture = CompletableFuture.supplyAsync(
                () -> feedRankingService.rankPosts(posts, userId, followedUserIds)
        );
        // Map feed surface -> ad placement so the reels tab requests full-screen
        // reel-ad creatives instead of landscape feed cards.
        final String adPlacement = "reels".equalsIgnoreCase(surface) ? "reel" : "feed";
        CompletableFuture<List<AdDeliveryResponse>> adsFuture = CompletableFuture.supplyAsync(
                () -> adServiceClient.fetchAds(adPlacement, userId, null, adCount * 2)
        );
        CompletableFuture<List<StoryPreviewDto>> storiesFuture = CompletableFuture.supplyAsync(
                () -> storyServiceClient.fetchDiscoverStories(viewerId)
        );

        List<PostDto> rankedPosts = rankedFuture.join();
        rankedPosts = feedRealtimeSignalService.applyOnlineSignals(userId, surface, rankedPosts);
        logger.debug("Ranked {} posts", rankedPosts.size());
        List<AdDeliveryResponse> ads = adsFuture.join();
        logger.debug("Fetched {} ads", ads.size());
        List<StoryPreviewDto> storyPreviews = storiesFuture.join();
        logger.debug("Fetched {} story previews for feed cluster", storyPreviews.size());

        // Step 6: Blend post subtypes (reels/posts) and merge ads.
        List<PostDto> mixedContent = mixPostAndReelContent(rankedPosts);
        List<FeedItem> feedItems = mergePostsAndAds(mixedContent, ads);
        // Home plan merge runs in getFeed() after Redis/cache so cached feeds stay current.
        feedItems = injectStoryCluster(feedItems, storyPreviews);

        logger.info("Computed feed with {} items ({} posts, {} ads)", 
                feedItems.size(),
                feedItems.stream().filter(item -> "post".equals(item.getType()) || "reel".equals(item.getType())).count(),
                feedItems.stream().filter(item -> "ad".equals(item.getType())).count());

        return feedItems;
    }

    /**
     * Fetch post candidates: prefer people the viewer follows; backfill with global discovery
     * when the followed slice is thin so the feed stays usable.
     */
    private List<PostDto> fetchPosts(
            int limit, String mood, List<UUID> followedUserIds, String viewerUserId) {
        try {
            int fetchLimit = Math.min(limit * 3, MAX_FEED_COMPUTATION_LIMIT);

            List<String> followedAuthorIds = followedUserIds.stream()
                    .map(UUID::toString)
                    .toList();

            if (followedAuthorIds.isEmpty()) {
                return postServiceClient.getPosts(1, fetchLimit, mood, null, viewerUserId);
            }

            List<PostDto> fromFollowed =
                    postServiceClient.getPosts(1, fetchLimit, mood, followedAuthorIds, viewerUserId);
            int minBeforeBackfill = Math.max(6, fetchLimit / 3);
            if (fromFollowed.size() >= minBeforeBackfill) {
                return fromFollowed;
            }

            List<PostDto> global = postServiceClient.getPosts(1, fetchLimit, mood, null, viewerUserId);
            List<PostDto> merged = mergeFollowedThenDiscovery(fromFollowed, global, fetchLimit);
            logger.debug(
                    "flow=feed_fetch_posts_backfill followedCount={} globalCount={} mergedCount={}",
                    fromFollowed.size(),
                    global.size(),
                    merged.size());
            return merged;
        } catch (Exception e) {
            logger.error("Error fetching posts: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Keeps followed-authors' posts first (recency order preserved per list), then appends
     * discovery posts not already present, capped at {@code maxSize}.
     */
    private List<PostDto> mergeFollowedThenDiscovery(
            List<PostDto> followed,
            List<PostDto> discovery,
            int maxSize) {
        Map<String, PostDto> byId = new LinkedHashMap<>();
        if (followed != null) {
            for (PostDto p : followed) {
                if (p != null && p.getId() != null && !p.getId().isBlank()) {
                    byId.putIfAbsent(p.getId(), p);
                }
            }
        }
        if (discovery != null) {
            for (PostDto p : discovery) {
                if (p != null && p.getId() != null && !p.getId().isBlank()) {
                    byId.putIfAbsent(p.getId(), p);
                }
            }
        }
        return byId.values().stream().limit(maxSize).collect(Collectors.toList());
    }

    /**
     * Suppress previously-shown posts. Content is classified into three buckets
     * by {@link FeedSeenService}:
     * <ul>
     *   <li><b>HARD</b> — seen very recently ({@code app.feed.seen.hard-filter-window-hours}):
     *       dropped outright so the same post doesn't come back in the next few requests.</li>
     *   <li><b>SOFT</b> — seen within the retention TTL but past the hard window:
     *       kept in the list but demoted so fresh content surfaces first; a soft
     *       post only reappears if the unseen pool is thin.</li>
     *   <li><b>UNSEEN</b> — untouched.</li>
     * </ul>
     * Ads and story clusters are never touched here.
     */
    private List<FeedItem> suppressSeenPosts(UUID userId, String surface, List<FeedItem> feedItems) {
        if (feedItems == null || feedItems.isEmpty()) {
            return feedItems;
        }

        Set<String> postIds = feedItems.stream()
                .filter(item -> ("post".equals(item.getType()) || "reel".equals(item.getType()))
                        && item.getPostId() != null)
                .map(FeedItem::getPostId)
                .collect(Collectors.toSet());

        if (postIds.isEmpty()) {
            return feedItems;
        }

        Map<String, SeenState> classification = feedSeenService.classifySeen(userId, surface, postIds);
        if (classification.isEmpty()) {
            return feedItems;
        }

        // Pass 1: hard-filter anything in the HARD window.
        List<FeedItem> afterHardFilter = new ArrayList<>(feedItems.size());
        List<FeedItem> softDemoted = new ArrayList<>();
        int hardCount = 0;
        int softCount = 0;
        for (FeedItem item : feedItems) {
            if (!"post".equals(item.getType()) && !"reel".equals(item.getType())) {
                afterHardFilter.add(item);
                continue;
            }
            SeenState state = classification.getOrDefault(item.getPostId(), SeenState.UNSEEN);
            if (state == SeenState.HARD) {
                hardCount++;
                continue;
            }
            if (state == SeenState.SOFT) {
                softCount++;
                softDemoted.add(item);
                continue;
            }
            afterHardFilter.add(item);
        }

        feedMetricsService.recordSeenSuppressed(surface, "hard", hardCount);
        feedMetricsService.recordSeenSuppressed(surface, "soft", softCount);

        if (softDemoted.isEmpty() || seenSoftPenalty <= 0.0d) {
            logger.debug("flow=seen_suppress userId={} surface={} hard={} soft={} soft_penalty={}",
                    userId, surface, hardCount, softCount, seenSoftPenalty);
            return afterHardFilter;
        }

        // Pass 2: append soft-demoted posts at a depth proportional to the
        // configured penalty. At penalty=1.0 they go to the end; at 0.35 they sit
        // ~35% deeper than they would have on score order alone.
        List<FeedItem> result = applySoftDemotion(afterHardFilter, softDemoted, seenSoftPenalty);
        logger.debug("flow=seen_suppress userId={} surface={} hard={} soft={} soft_penalty={} size_before={} size_after={}",
                userId, surface, hardCount, softCount, seenSoftPenalty, feedItems.size(), result.size());
        return result;
    }

    /**
     * Soft-demote: the demoted items keep their relative order but are inserted
     * below {@code penalty * fresh.size()} unseen items. Ads and story clusters
     * pass through untouched.
     */
    private List<FeedItem> applySoftDemotion(List<FeedItem> fresh, List<FeedItem> demoted, double penalty) {
        if (demoted.isEmpty()) {
            return fresh;
        }
        long freshContent = fresh.stream()
                .filter(item -> "post".equals(item.getType()) || "reel".equals(item.getType()))
                .count();
        if (freshContent == 0) {
            // Nothing fresh to compare against — append demoted posts at the end.
            List<FeedItem> out = new ArrayList<>(fresh.size() + demoted.size());
            out.addAll(fresh);
            out.addAll(demoted);
            return out;
        }
        // Skip this many content slots before inserting demoted items.
        int skipContent = (int) Math.min(freshContent, Math.round(freshContent * penalty));
        List<FeedItem> out = new ArrayList<>(fresh.size() + demoted.size());
        int seenContent = 0;
        boolean injected = false;
        for (FeedItem item : fresh) {
            out.add(item);
            if ("post".equals(item.getType()) || "reel".equals(item.getType())) {
                seenContent++;
            }
            if (!injected && seenContent >= skipContent) {
                out.addAll(demoted);
                injected = true;
            }
        }
        if (!injected) {
            out.addAll(demoted);
        }
        return out;
    }

    /**
     * Apply cursor-based pagination to the post-filter feed.
     *
     * <p>Resolution order:</p>
     * <ol>
     *   <li><b>anchor lookup</b> — locate {@code cursor.lastPostId} inside the
     *       current ranked slice and continue from the next item. This is the
     *       happy path as long as the anchor survived seen-filter suppression.</li>
     *   <li><b>position fallback</b> — when the anchor has been filtered out
     *       (e.g. tapped to "see more" since last request), we resume from
     *       {@code cursor.position} so the user never sees duplicates from
     *       page 1 again.</li>
     *   <li><b>first page</b> — cursor missing entirely.</li>
     * </ol>
     *
     * <p>Returns a {@link CursorSlice} so the caller has both the items and
     * the resulting absolute position it needs to mint the next cursor.</p>
     */
    private CursorSlice applyCursorPagination(List<FeedItem> feedItems, FeedCursor cursor, int limit) {
        if (feedItems == null || feedItems.isEmpty()) {
            return new CursorSlice(List.of(), 0, feedItems == null ? 0 : feedItems.size());
        }

        int startIndex = 0;
        if (cursor != null) {
            int anchor = indexOfAnchor(feedItems, cursor.getLastPostId());
            if (anchor >= 0) {
                startIndex = anchor + 1;
            } else {
                // Anchor vanished (likely now seen-suppressed). Continue from
                // the last absolute position we served so we don't repeat the
                // earlier pages.
                startIndex = Math.min(cursor.getPosition(), feedItems.size());
            }
        }

        if (startIndex >= feedItems.size()) {
            return new CursorSlice(List.of(), startIndex, feedItems.size());
        }

        int endIndex = Math.min(startIndex + limit, feedItems.size());
        List<FeedItem> slice = new ArrayList<>(feedItems.subList(startIndex, endIndex));
        return new CursorSlice(slice, endIndex, feedItems.size());
    }

    /**
     * Walk the feed once looking for the anchor id. Ads and story clusters
     * use their own ids, so we check every item's cursor-id — not just
     * post-typed items.
     */
    private int indexOfAnchor(List<FeedItem> feedItems, String anchorId) {
        if (anchorId == null || anchorId.isBlank()) return -1;
        for (int i = 0; i < feedItems.size(); i++) {
            if (anchorId.equals(getCursorItemId(feedItems.get(i)))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Mint the next {@link FeedResponse}. A new cursor is emitted only when
     * {@code slice} has more pages behind it — i.e. there are still items
     * past {@code nextPosition} in the in-memory feed. The cursor embeds both
     * the last served item id and its absolute position so follow-up requests
     * resume correctly even if seen state shifts between them.
     */
    private FeedResponse buildFeedResponse(CursorSlice slice, int limit, String surface, String mood) {
        boolean hasMore = slice.items.size() >= limit && slice.nextPosition < slice.totalAvailable;
        String nextCursor = null;

        if (hasMore && !slice.items.isEmpty()) {
            FeedItem lastItem = slice.items.get(slice.items.size() - 1);
            String anchorId = getCursorItemId(lastItem);
            nextCursor = feedCursorCodec.encode(FeedCursor.of(
                    anchorId,
                    slice.nextPosition,
                    surface,
                    mood,
                    Instant.now().toEpochMilli()
            ));
        }

        return new FeedResponse(
                slice.items,
                nextCursor,
                hasMore,
                (long) slice.items.size(),
                recommendedPrefetchLimit
        );
    }

    /**
     * Cursor resolution result: the {@code items} served on this page plus
     * the absolute offset into the pre-pagination feed so we can encode the
     * next cursor without re-scanning.
     */
    private static final class CursorSlice {
        final List<FeedItem> items;
        final int nextPosition;
        final int totalAvailable;

        CursorSlice(List<FeedItem> items, int nextPosition, int totalAvailable) {
            this.items = items;
            this.nextPosition = nextPosition;
            this.totalAvailable = totalAvailable;
        }
    }

    /**
     * Calculate how many ads should be inserted based on post count.
     */
    private int calculateAdCount(int postCount) {
        if (postCount <= MIN_POSTS_BETWEEN_ADS) {
            return 0;
        }
        return (int) Math.ceil((double) postCount / ((MIN_POSTS_BETWEEN_ADS + MAX_POSTS_BETWEEN_ADS) / 2.0));
    }

    /**
     * Mix post subtypes into a more varied stream (e.g., reel + regular post),
     * so feed feels less blocky and closer to production social apps.
     */
    private List<PostDto> mixPostAndReelContent(List<PostDto> rankedPosts) {
        if (rankedPosts == null || rankedPosts.isEmpty()) {
            return Collections.emptyList();
        }

        List<PostDto> reels = new ArrayList<>();
        List<PostDto> regular = new ArrayList<>();
        for (PostDto post : rankedPosts) {
            if (post == null) {
                continue;
            }
            String type = post.getPostType();
            if (type != null && "reel".equalsIgnoreCase(type)) {
                reels.add(post);
            } else {
                regular.add(post);
            }
        }

        if (reels.isEmpty() || regular.isEmpty()) {
            return rankedPosts;
        }

        List<PostDto> mixed = new ArrayList<>(rankedPosts.size());
        int regularStreak = 0;
        int reelIndex = 0;
        int regularIndex = 0;

        while (reelIndex < reels.size() || regularIndex < regular.size()) {
            boolean shouldInsertReel = regularStreak >= 3 && reelIndex < reels.size();

            if (!shouldInsertReel && regularIndex < regular.size()) {
                mixed.add(regular.get(regularIndex++));
                regularStreak++;
                continue;
            }
            if (reelIndex < reels.size()) {
                mixed.add(reels.get(reelIndex++));
                regularStreak = 0;
                continue;
            }
            if (regularIndex < regular.size()) {
                mixed.add(regular.get(regularIndex++));
                regularStreak++;
            }
        }

        return mixed;
    }

    /**
     * Merge posts and ads with insertion logic.
     */
    private List<FeedItem> mergePostsAndAds(List<PostDto> posts, List<AdDeliveryResponse> ads) {
        if (ads.isEmpty()) {
            return posts.stream()
                    .map(this::toFeedPostItem)
                    .collect(Collectors.toList());
        }

        List<FeedItem> feedItems = new ArrayList<>();
        int adIndex = 0;
        int postsSinceLastAd = 0;
        UUID lastAdId = null;
        Random random = new Random();

        for (PostDto post : posts) {
            feedItems.add(toFeedPostItem(post));
            postsSinceLastAd++;

            if (postsSinceLastAd >= MIN_POSTS_BETWEEN_ADS) {
                int postsUntilAd = MIN_POSTS_BETWEEN_ADS + random.nextInt(MAX_POSTS_BETWEEN_ADS - MIN_POSTS_BETWEEN_ADS + 1);
                
                if (postsSinceLastAd >= postsUntilAd && adIndex < ads.size()) {
                    AdDeliveryResponse adToInsert = findNextNonDuplicateAd(ads, lastAdId, adIndex);
                    
                    if (adToInsert != null) {
                        feedItems.add(FeedItem.fromAd(adToInsert));
                        lastAdId = adToInsert.adId();
                        adIndex = ads.indexOf(adToInsert) + 1;
                        postsSinceLastAd = 0;
                    }
                }
            }
        }

        return feedItems;
    }

    private FeedItem toFeedPostItem(PostDto post) {
        String postType = post != null ? post.getPostType() : null;
        if (postType != null && "reel".equalsIgnoreCase(postType)) {
            return FeedItem.fromReel(post);
        }
        return FeedItem.fromPost(post);
    }

    private String getCursorItemId(FeedItem item) {
        if (item == null) {
            return null;
        }
        if ("ad".equals(item.getType())) {
            return item.getAdId();
        }
        if ("story_cluster".equals(item.getType())) {
            return item.getStoryClusterId();
        }
        if ("plan".equals(item.getType())) {
            return item.getPlanId();
        }
        return item.getPostId();
    }

    /**
     * Blends persisted {@code circle_plans} into the home feed: after every Nth post/reel,
     * inserts the next newest plan. Existing {@code plan} rows are stripped first so re-injection
     * after a Redis hit stays idempotent.
     */
    private List<FeedItem> injectHomePlans(List<FeedItem> items, String surface, int computationLimit) {
        if (items == null || !"home".equalsIgnoreCase(surface)) {
            return items;
        }
        List<FeedItem> base = new ArrayList<>(items.size());
        for (FeedItem it : items) {
            if (!"plan".equals(it.getType())) {
                base.add(it);
            }
        }
        int poolSize = Math.min(32, Math.max(8, computationLimit / 5));
        List<PlanResponse> plans = planService.listRecentPlansForFeed(poolSize);
        if (plans.isEmpty()) {
            return base.isEmpty() ? items : base;
        }
        Iterator<PlanResponse> planIt = plans.iterator();
        List<FeedItem> out = new ArrayList<>(base.size() + Math.min(plans.size(), base.size() / 4 + 2));
        int contentSlots = 0;
        for (FeedItem item : base) {
            out.add(item);
            if ("post".equals(item.getType()) || "reel".equals(item.getType())) {
                contentSlots++;
                if (contentSlots % HOME_FEED_PLAN_EVERY_N_CONTENT == 0 && planIt.hasNext()) {
                    out.add(FeedItem.fromPlan(planIt.next()));
                }
            }
        }
        return out;
    }

    private List<FeedItem> injectStoryCluster(List<FeedItem> feedItems, List<StoryPreviewDto> discoverStories) {
        if (feedItems == null || feedItems.isEmpty()) {
            return feedItems;
        }
        boolean alreadyPresent = feedItems.stream().anyMatch(i -> "story_cluster".equals(i.getType()));
        if (alreadyPresent) {
            return feedItems;
        }
        List<StoryPreviewDto> trimmed = trimStoryPreviews(discoverStories);
        List<FeedItem> withStoryCluster = new ArrayList<>(feedItems.size() + 1);
        int insertAt = Math.min(2, feedItems.size());
        for (int i = 0; i < feedItems.size(); i++) {
            if (i == insertAt) {
                withStoryCluster.add(FeedItem.storyCluster("home-stories", trimmed));
            }
            withStoryCluster.add(feedItems.get(i));
        }
        if (insertAt == feedItems.size()) {
            withStoryCluster.add(FeedItem.storyCluster("home-stories", trimmed));
        }
        return withStoryCluster;
    }

    private List<StoryPreviewDto> trimStoryPreviews(List<StoryPreviewDto> in) {
        if (in == null || in.isEmpty()) {
            return List.of();
        }
        if (in.size() <= MAX_STORY_PREVIEWS_IN_FEED) {
            return in;
        }
        return new ArrayList<>(in.subList(0, MAX_STORY_PREVIEWS_IN_FEED));
    }

    private AdDeliveryResponse findNextNonDuplicateAd(List<AdDeliveryResponse> ads, UUID lastAdId, int startIndex) {
        if (lastAdId == null) {
            return startIndex < ads.size() ? ads.get(startIndex) : null;
        }

        for (int i = startIndex; i < ads.size(); i++) {
            AdDeliveryResponse ad = ads.get(i);
            if (!lastAdId.equals(ad.adId())) {
                return ad;
            }
        }

        if (ads.size() > 1) {
            for (int i = 0; i < startIndex && i < ads.size(); i++) {
                AdDeliveryResponse ad = ads.get(i);
                if (!lastAdId.equals(ad.adId())) {
                    return ad;
                }
            }
        }

        return null;
    }
}
