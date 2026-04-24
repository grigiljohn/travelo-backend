package com.travelo.feedservice.service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which posts each user has been shown on each surface.
 *
 * <p>Backed by Redis {@code ZSET}s where the score is the epoch-seconds the
 * post was first seen, the member is the post id, and the key is
 * {@code seen:ts:{userId}:{surface}}. Storing the timestamp lets callers
 * distinguish between:
 * <ul>
 *   <li>{@link SeenState#HARD} — very recently seen; should be filtered out
 *       of the next feed response.</li>
 *   <li>{@link SeenState#SOFT} — seen within the retention TTL but past the
 *       hard-filter window; should be softly demoted but may resurface when
 *       fresh content is thin.</li>
 *   <li>{@link SeenState#UNSEEN} — never shown (or already expired).</li>
 * </ul>
 *
 * <p>Redis failures degrade gracefully: writes silently swallow and reads
 * return "unseen" so a broken cache never blocks the feed.
 */
public interface FeedSeenService {

    enum SeenState { UNSEEN, SOFT, HARD }

    /**
     * Mark posts as seen for {@code userId} on {@code surface}. Uses
     * {@code ZADD NX} so an earlier seen-at timestamp is never overwritten.
     */
    void markPostsAsSeen(UUID userId, String surface, Set<String> postIds);

    /**
     * Classify each input {@code postId} as UNSEEN, SOFT, or HARD. Unknown
     * ids map to {@link SeenState#UNSEEN}.
     */
    Map<String, SeenState> classifySeen(UUID userId, String surface, Set<String> postIds);

    /**
     * Subset of {@code postIds} currently in {@link SeenState#HARD} or
     * {@link SeenState#SOFT} (i.e., anything present in the ZSET). Kept for
     * callers that only care about "was this shown".
     */
    Set<String> getSeenPostIds(UUID userId, String surface, Set<String> postIds);

    /**
     * Point lookup. Returns {@code true} if the post is in either HARD or
     * SOFT state.
     */
    boolean isPostSeen(UUID userId, String surface, String postId);

    /**
     * Clear all tracked posts for {@code userId} on {@code surface}.
     */
    void clearSeenPosts(UUID userId, String surface);
}
