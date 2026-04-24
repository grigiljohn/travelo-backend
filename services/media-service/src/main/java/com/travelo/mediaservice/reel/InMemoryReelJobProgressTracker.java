package com.travelo.mediaservice.reel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single-node tracker. Entries are pruned lazily on every access after {@link #TTL}.
 * Wired as the {@link ReelJobProgressTracker} bean when
 * {@code reel.progress.backend=memory} (default).
 */
public class InMemoryReelJobProgressTracker implements ReelJobProgressTracker {

    private static final Logger log = LoggerFactory.getLogger(InMemoryReelJobProgressTracker.class);
    private static final Duration TTL = Duration.ofMinutes(15);

    private final Map<String, Snapshot> snapshots = new ConcurrentHashMap<>();
    private final ReelJobProgressBroker broker;

    public InMemoryReelJobProgressTracker() {
        this(null);
    }

    public InMemoryReelJobProgressTracker(ReelJobProgressBroker broker) {
        this.broker = broker;
    }

    @Override
    public void report(String jobId, ReelJobStage stage, String message, Integer percent) {
        if (jobId == null || jobId.isBlank()) {
            return;
        }
        Integer clamped = percent == null ? null : Math.max(0, Math.min(100, percent));
        Snapshot existing = snapshots.get(jobId);
        if (existing != null && existing.stage() == stage
                && existing.percent() != null && clamped != null
                && clamped < existing.percent()) {
            clamped = existing.percent();
        }
        Snapshot next = new Snapshot(stage, message, clamped, Instant.now());
        snapshots.put(jobId, next);
        prune();
        if (broker != null) {
            broker.publish(jobId, next);
        }
    }

    @Override
    public Snapshot get(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return null;
        }
        prune();
        return snapshots.get(jobId);
    }

    private void prune() {
        Instant cutoff = Instant.now().minus(TTL);
        try {
            Iterator<Map.Entry<String, Snapshot>> it = snapshots.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Snapshot> e = it.next();
                if (e.getValue() == null || e.getValue().updatedAt().isBefore(cutoff)) {
                    it.remove();
                }
            }
        } catch (Exception ex) {
            log.debug("progress tracker prune failed: {}", ex.toString());
        }
    }
}
