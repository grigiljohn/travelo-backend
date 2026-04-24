package com.travelo.postservice.realtime;

import java.io.Closeable;
import java.util.function.Consumer;

/**
 * Real-time fan-out of newly-created comments to SSE subscribers, keyed by
 * {@code postId}. Decoupled from the JPA write path so {@code POST /comments}
 * returns as fast as possible; broker failures are swallowed by implementations.
 *
 * <p>Two swappable backends (selected by {@code comments.stream.backend}):
 * <ul>
 *   <li>{@code memory} (default) — same-JVM fan-out, good for single-node dev.</li>
 *   <li>{@code redis} — Redis Pub/Sub, required when running multiple replicas
 *       of social-service so that a comment saved on replica A reaches a
 *       browser/mobile SSE stream attached to replica B.</li>
 * </ul>
 */
public interface CommentStreamBroker {

    /**
     * Publish {@code event} to all current subscribers of {@code postId}.
     * Implementations must never throw into the caller's thread.
     */
    void publish(String postId, CommentStreamEvent event);

    /**
     * Attach a listener for {@code postId}. Closing the returned handle
     * unregisters the listener and releases any underlying resources
     * (e.g. Redis channel subscriptions).
     */
    Subscription subscribe(String postId, Consumer<CommentStreamEvent> listener);

    /** Handle for an active subscription. {@link #close()} is idempotent. */
    interface Subscription extends Closeable {
        @Override
        void close();
    }
}
