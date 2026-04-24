package com.travelo.mediaservice.reel;

import java.time.Instant;

/**
 * Thread-safe store of reel FFmpeg pipeline progress keyed by a client-supplied job id.
 * <p>
 * Has two swappable implementations picked at startup via {@code reel.progress.backend}:
 * <ul>
 *   <li>{@link InMemoryReelJobProgressTracker} (default {@code memory}) — single node, dev.</li>
 *   <li>{@link RedisReelJobProgressTracker} ({@code redis}) — multi-node production.</li>
 * </ul>
 * Implementations MUST:
 * <ul>
 *   <li>Clamp percent into {@code [0..100]}.</li>
 *   <li>Refuse percent regressions while the stage is unchanged (UI monotonicity).</li>
 *   <li>Expire entries after ~15 minutes so the store stays bounded.</li>
 * </ul>
 */
public interface ReelJobProgressTracker {

    default void report(String jobId, ReelJobStage stage) {
        report(jobId, stage, null, null);
    }

    default void report(String jobId, ReelJobStage stage, String message) {
        report(jobId, stage, message, null);
    }

    void report(String jobId, ReelJobStage stage, String message, Integer percent);

    default void reportFailure(String jobId, String message) {
        report(jobId, ReelJobStage.FAILED, message, 100);
    }

    Snapshot get(String jobId);

    record Snapshot(ReelJobStage stage, String message, Integer percent, Instant updatedAt) {}
}
