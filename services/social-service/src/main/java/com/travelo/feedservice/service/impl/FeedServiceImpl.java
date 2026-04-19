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
import com.travelo.feedservice.service.FeedCacheService;
import com.travelo.feedservice.service.FeedMetricsService;
import com.travelo.feedservice.service.FeedRealtimeSignalService;
import com.travelo.feedservice.service.FeedRankingService;
import com.travelo.feedservice.service.FeedSeenService;
import com.travelo.feedservice.service.FeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
    
    private final boolean cacheEnabled;
    private final boolean fanOutEnabled;

    // Ad insertion configuration
    private static final int MIN_POSTS_BETWEEN_ADS = 5;
    private static final int MAX_POSTS_BETWEEN_ADS = 7;
    
    // Feed computation limits
    private static final int MAX_FEED_COMPUTATION_LIMIT = 500;
    private static final int MAX_STORY_PREVIEWS_IN_FEED = 24;

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
            @Value("${app.feed.cache-enabled:true}") boolean cacheEnabled,
            @Value("${app.feed.fan-out-enabled:true}") boolean fanOutEnabled,
            @Value("${app.feed.recommended-prefetch-limit:12}") int recommendedPrefetchLimit) {
        this.postServiceClient = postServiceClient;
        this.adServiceClient = adServiceClient;
        this.storyServiceClient = storyServiceClient;
        this.userServiceClient = userServiceClient;
        this.feedRankingService = feedRankingService;
        this.feedRealtimeSignalService = feedRealtimeSignalService;
        this.feedMetricsService = feedMetricsService;
        this.feedCacheService = feedCacheService;
        this.feedSeenService = feedSeenService;
        this.cacheEnabled = cacheEnabled;
        this.fanOutEnabled = fanOutEnabled;
        this.recommendedPrefetchLimit = recommendedPrefetchLimit;
    }

    @Override
    public FeedResponse getFeed(UUID userId, String cursor, int limit, String mood, String surface) {
        return feedMetricsService.timeGetFeed(surface, () -> {
            logger.info("Getting feed for userId={}, cursor={}, limit={}, mood={}, surface={}",
                    userId, cursor, limit, mood, surface);

        // Fetch more items to account for seen filtering and ads
        // Buffer size: 2x limit to ensure we have enough after filtering
        int fetchLimit = limit * 3; // Account for seen filtering (some posts may be filtered out)
        
        // Compute or get feed items
        List<FeedItem> feedItems;
        
        // Try to get from cache first (but we still need to filter seen posts)
        if (cacheEnabled && feedCacheService.isFeedCached(userId)) {
            // Always hydrate from the top of cached ranked feed; cursor pagination is applied by item ID later.
            List<FeedItem> cachedItems = feedCacheService.getCachedFeed(userId, null, fetchLimit);
            if (!cachedItems.isEmpty()) {
                logger.debug("Retrieved {} items from cache", cachedItems.size());
                feedItems = cachedItems;
            } else {
                // Cache miss or insufficient items, compute feed
                feedItems = computeFeed(userId, fetchLimit, mood, surface);
                // Cache the computed feed (without seen filtering, as seen state changes)
                if (cacheEnabled && feedItems.size() > 0) {
                    feedCacheService.cacheFeed(userId, feedItems);
                }
            }
        } else {
            // Compute feed on the fly
            feedItems = computeFeed(userId, fetchLimit, mood, surface);
            // Cache the computed feed (without seen filtering)
            if (cacheEnabled && feedItems.size() > 0) {
                feedCacheService.cacheFeed(userId, feedItems);
                logger.debug("Cached {} feed items for user {}", feedItems.size(), userId);
            }
        }

        // CRITICAL: Filter seen posts BEFORE cursor pagination
        // This ensures cursor pagination works correctly with seen filtering
        feedItems = filterSeenPosts(userId, surface, feedItems);
        feedItems = feedRealtimeSignalService.applySessionFatigue(userId, surface, feedItems);

        // Apply cursor pagination AFTER filtering
        List<FeedItem> paginatedItems = applyCursorPagination(feedItems, cursor, limit);
        feedRealtimeSignalService.recordServedItems(userId, surface, paginatedItems);
        
            long contentCount = paginatedItems.stream()
                    .filter(item -> "post".equals(item.getType()) || "reel".equals(item.getType()))
                    .count();
            long adCount = paginatedItems.stream()
                    .filter(item -> "ad".equals(item.getType()))
                    .count();
            feedMetricsService.recordFeedServed(
                    surface,
                    paginatedItems.size(),
                    (int) contentCount,
                    (int) adCount
            );
            return buildFeedResponse(paginatedItems, limit);
        });
    }

    @Override
    public FeedRankingDebugResponse debugRanking(UUID userId, int limit, String mood, String surface) {
        int fetchLimit = Math.min(Math.max(limit, 1) * 3, MAX_FEED_COMPUTATION_LIMIT);
        List<UUID> followedUserIds = userServiceClient.getFollowing(userId);
        Set<String> followedSet = followedUserIds.stream().map(UUID::toString).collect(Collectors.toSet());
        List<PostDto> posts = fetchPosts(fetchLimit, mood, followedUserIds);
        List<PostDto> ranked = feedRankingService.rankPosts(posts, userId, followedUserIds);

        List<FeedRankingDebugItem> debugItems = ranked.stream()
                .limit(Math.max(1, limit))
                .map(p -> {
                    boolean following = p != null && followedSet.contains(p.getUserId());
                    FeedRankingDebugItem item = feedRankingService.buildDebugItem(p, userId, following);
                    double onlineSignal = feedRealtimeSignalService.getOnlineSignalScore(userId, surface, p.getId());
                    item.setOnlineSignalScore(onlineSignal);
                    item.setFinalScore(item.getBaseScore() + onlineSignal * 0.25d);
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
        List<PostDto> posts = fetchPosts(limit, mood, followedUserIds);
        logger.debug("Fetched {} posts", posts.size());

        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 3/4/5 in parallel: rank posts + fetch ads + discover stories (fail-open for ads/stories).
        int adCount = calculateAdCount(posts.size());
        String viewerId = userId.toString();
        CompletableFuture<List<PostDto>> rankedFuture = CompletableFuture.supplyAsync(
                () -> feedRankingService.rankPosts(posts, userId, followedUserIds)
        );
        CompletableFuture<List<AdDeliveryResponse>> adsFuture = CompletableFuture.supplyAsync(
                () -> adServiceClient.fetchAds("feed", userId, null, adCount * 2)
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
    private List<PostDto> fetchPosts(int limit, String mood, List<UUID> followedUserIds) {
        try {
            int fetchLimit = Math.min(limit * 3, MAX_FEED_COMPUTATION_LIMIT);

            List<String> followedAuthorIds = followedUserIds.stream()
                    .map(UUID::toString)
                    .toList();

            if (followedAuthorIds.isEmpty()) {
                return postServiceClient.getPosts(1, fetchLimit, mood, null);
            }

            List<PostDto> fromFollowed = postServiceClient.getPosts(1, fetchLimit, mood, followedAuthorIds);
            int minBeforeBackfill = Math.max(6, fetchLimit / 3);
            if (fromFollowed.size() >= minBeforeBackfill) {
                return fromFollowed;
            }

            List<PostDto> global = postServiceClient.getPosts(1, fetchLimit, mood, null);
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
     * Filter out posts that have been seen by the user on the given surface.
     * Only filters posts, not ads (ads don't participate in seen-once logic).
     * 
     * @param userId User ID
     * @param surface Surface name (e.g., "home", "explore", "reels")
     * @param feedItems Feed items to filter
     * @return Filtered feed items (only unseen posts, all ads preserved)
     */
    private List<FeedItem> filterSeenPosts(UUID userId, String surface, List<FeedItem> feedItems) {
        if (feedItems == null || feedItems.isEmpty()) {
            return feedItems;
        }
        
        // Extract post IDs from feed items (only posts, not ads)
        Set<String> postIds = feedItems.stream()
                .filter(item -> ("post".equals(item.getType()) || "reel".equals(item.getType())) && item.getPostId() != null)
                .map(FeedItem::getPostId)
                .collect(Collectors.toSet());
        
        if (postIds.isEmpty()) {
            return feedItems; // No posts to filter
        }
        
        // Get seen post IDs (O(1) per post using Redis SET)
        Set<String> seenPostIds = feedSeenService.getSeenPostIds(userId, surface, postIds);
        
        if (seenPostIds.isEmpty()) {
            logger.debug("No seen posts to filter for user {} on surface {}", userId, surface);
            return feedItems; // No seen posts, return as-is
        }
        
        // Filter out seen posts
        List<FeedItem> filteredItems = feedItems.stream()
                .filter(item -> {
                    // Keep ads (they don't participate in seen-once)
                    if ("ad".equals(item.getType()) || "story_cluster".equals(item.getType())) {
                        return true;
                    }
                    // Keep posts/reels that haven't been seen
                    return item.getPostId() == null || !seenPostIds.contains(item.getPostId());
                })
                .collect(Collectors.toList());
        
        int filteredCount = feedItems.size() - filteredItems.size();
        logger.debug("Filtered out {} seen posts for user {} on surface {} ({} remaining)", 
                filteredCount, userId, surface, filteredItems.size());
        
        return filteredItems;
    }

    /**
     * Apply cursor-based pagination to feed items.
     * Cursor must include post_id or ad_id to ensure consistency after seen filtering.
     */
    private List<FeedItem> applyCursorPagination(List<FeedItem> feedItems, String cursor, int limit) {
        if (cursor == null || cursor.isEmpty()) {
            // Return first page
            return feedItems.stream().limit(limit).collect(Collectors.toList());
        }

        // Find starting index from cursor (cursor is post ID or ad ID)
        int startIndex = 0;
        for (int i = 0; i < feedItems.size(); i++) {
            FeedItem item = feedItems.get(i);
            String itemId = getCursorItemId(item);
            if (cursor.equals(itemId)) {
                startIndex = i + 1;
                break;
            }
        }

        // Return next page
        int endIndex = Math.min(startIndex + limit, feedItems.size());
        if (startIndex >= feedItems.size()) {
            return Collections.emptyList();
        }

        return feedItems.subList(startIndex, endIndex);
    }

    /**
     * Build FeedResponse with next cursor.
     */
    private FeedResponse buildFeedResponse(List<FeedItem> items, int limit) {
        String nextCursor = null;
        boolean hasMore = items.size() >= limit;

        if (hasMore && !items.isEmpty()) {
            FeedItem lastItem = items.get(items.size() - 1);
            nextCursor = getCursorItemId(lastItem);
        }

        return new FeedResponse(
                items,
                nextCursor,
                hasMore,
                (long) items.size(),
                recommendedPrefetchLimit
        );
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
        return item.getPostId();
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
