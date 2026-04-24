package com.travelo.mediaservice.reel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Same-JVM fan-out broker. {@link #publish} iterates a copy-on-write listener list
 * per job id; {@link #subscribe} returns a handle that removes the listener on close.
 * <p>
 * Listener exceptions are logged and swallowed so a misbehaving consumer can't
 * disrupt the pipeline or other subscribers.
 */
public class InMemoryReelJobProgressBroker implements ReelJobProgressBroker {

    private static final Logger log = LoggerFactory.getLogger(InMemoryReelJobProgressBroker.class);

    private final Map<String, List<Consumer<ReelJobProgressTracker.Snapshot>>> listeners =
            new ConcurrentHashMap<>();

    @Override
    public void publish(String jobId, ReelJobProgressTracker.Snapshot snapshot) {
        if (jobId == null || jobId.isBlank() || snapshot == null) {
            return;
        }
        List<Consumer<ReelJobProgressTracker.Snapshot>> list = listeners.get(jobId);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (Consumer<ReelJobProgressTracker.Snapshot> l : list) {
            try {
                l.accept(snapshot);
            } catch (Exception e) {
                log.warn("in-memory broker listener failed jobId={}: {}", jobId, e.toString());
            }
        }
    }

    @Override
    public Subscription subscribe(String jobId, Consumer<ReelJobProgressTracker.Snapshot> listener) {
        if (jobId == null || jobId.isBlank() || listener == null) {
            return () -> { /* no-op */ };
        }
        List<Consumer<ReelJobProgressTracker.Snapshot>> list =
                listeners.computeIfAbsent(jobId, k -> new CopyOnWriteArrayList<>());
        list.add(listener);
        return () -> {
            List<Consumer<ReelJobProgressTracker.Snapshot>> current = listeners.get(jobId);
            if (current != null) {
                current.remove(listener);
                if (current.isEmpty()) {
                    listeners.remove(jobId, current);
                }
            }
        };
    }
}
