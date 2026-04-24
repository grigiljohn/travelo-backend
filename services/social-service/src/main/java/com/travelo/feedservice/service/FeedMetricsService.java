package com.travelo.feedservice.service;

import java.util.concurrent.Callable;

public interface FeedMetricsService {

    <T> T timeGetFeed(String surface, Callable<T> callable);

    void recordFeedServed(String surface, int totalItems, int postOrReelItems, int adItems);

    void recordFallbackUsed(String dependency, String mode);

    void recordOnlineSignalEvent(String eventType);

    void recordFatigueReorder(String surface, int reorderedCount);

    /**
     * Record how many posts were suppressed by the seen tracker during a single
     * feed response, split by {@code mode} ({@code hard} filter vs {@code soft}
     * demote).
     */
    void recordSeenSuppressed(String surface, String mode, int count);
}

