package com.travelo.mediaservice.reel;

import java.io.Closeable;
import java.util.function.Consumer;

/**
 * Real-time fan-out of reel pipeline snapshots to subscribers (SSE endpoints,
 * admin dashboards, etc.). Decoupled from {@link ReelJobProgressTracker}'s
 * durable store so clients can consume "push" updates instead of polling.
 * <p>
 * Two swappable implementations, chosen by the same {@code reel.progress.backend}
 * property that picks the tracker:
 * <ul>
 *   <li>{@link InMemoryReelJobProgressBroker} ({@code memory}) — same-JVM listeners.</li>
 *   <li>{@link RedisReelJobProgressBroker} ({@code redis}) — Redis Pub/Sub,
 *       lets any replica stream updates from any other replica's processing.</li>
 * </ul>
 */
public interface ReelJobProgressBroker {

    /**
     * Publish a snapshot to all current subscribers of {@code jobId}.
     * Implementations must never throw into the caller's thread.
     */
    void publish(String jobId, ReelJobProgressTracker.Snapshot snapshot);

    /**
     * Attach a listener for {@code jobId}. Closing the returned handle
     * unregisters the listener and releases any underlying resources
     * (e.g. Redis channel subscriptions).
     */
    Subscription subscribe(String jobId, Consumer<ReelJobProgressTracker.Snapshot> listener);

    /** Handle for an active subscription. {@link #close()} is idempotent. */
    interface Subscription extends Closeable {
        @Override
        void close();
    }
}
