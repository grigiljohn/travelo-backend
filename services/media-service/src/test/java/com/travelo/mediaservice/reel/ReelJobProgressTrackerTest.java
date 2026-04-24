package com.travelo.mediaservice.reel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies the shared {@link ReelJobProgressTracker} contract on the in-memory impl.
 * The Redis impl is covered by {@code RedisReelJobProgressTrackerTest}.
 */
class ReelJobProgressTrackerTest {

    private ReelJobProgressTracker newTracker() {
        return new InMemoryReelJobProgressTracker();
    }

    @Test
    void unknownJobReturnsNullSnapshot() {
        assertNull(newTracker().get("missing"));
    }

    @Test
    void reportStoresLatestStageForJob() {
        ReelJobProgressTracker tracker = newTracker();
        tracker.report("job-1", ReelJobStage.OPTIMIZING, "trim");
        tracker.report("job-1", ReelJobStage.FILTERING, "filter");

        ReelJobProgressTracker.Snapshot snap = tracker.get("job-1");
        assertNotNull(snap);
        assertEquals(ReelJobStage.FILTERING, snap.stage());
        assertEquals("filter", snap.message());
    }

    @Test
    void reportFailureSetsFailedStage() {
        ReelJobProgressTracker tracker = newTracker();
        tracker.reportFailure("job-2", "boom");

        ReelJobProgressTracker.Snapshot snap = tracker.get("job-2");
        assertNotNull(snap);
        assertEquals(ReelJobStage.FAILED, snap.stage());
        assertEquals("boom", snap.message());
    }

    @Test
    void blankJobIdIgnoredOnWriteAndRead() {
        ReelJobProgressTracker tracker = newTracker();
        tracker.report(null, ReelJobStage.QUEUED);
        tracker.report("", ReelJobStage.QUEUED);
        assertNull(tracker.get(null));
        assertNull(tracker.get(""));
    }

    @Test
    void isolatesDifferentJobIds() {
        ReelJobProgressTracker tracker = newTracker();
        tracker.report("a", ReelJobStage.MUSIC);
        tracker.report("b", ReelJobStage.READY);

        assertEquals(ReelJobStage.MUSIC, tracker.get("a").stage());
        assertEquals(ReelJobStage.READY, tracker.get("b").stage());
    }

    @Test
    void explicitPercentIsPersistedAndClampedToRange() {
        ReelJobProgressTracker tracker = newTracker();
        tracker.report("job-3", ReelJobStage.FILTERING, "encoding", 42);
        assertEquals(42, tracker.get("job-3").percent());

        tracker.report("job-3", ReelJobStage.FILTERING, "encoding", 150);
        assertEquals(100, tracker.get("job-3").percent());

        tracker.report("job-3", ReelJobStage.FILTERING, "encoding", -20);
        assertEquals(100, tracker.get("job-3").percent());
    }

    @Test
    void percentDoesNotRegressWithinSameStage() {
        ReelJobProgressTracker tracker = newTracker();
        tracker.report("job-4", ReelJobStage.FILTERING, null, 55);
        tracker.report("job-4", ReelJobStage.FILTERING, null, 40);
        assertEquals(55, tracker.get("job-4").percent(),
                "percent must be monotonic within a stage to avoid jittery UI");
    }

    @Test
    void percentResetsWhenStageAdvances() {
        ReelJobProgressTracker tracker = newTracker();
        tracker.report("job-5", ReelJobStage.FILTERING, null, 70);
        tracker.report("job-5", ReelJobStage.MUSIC, null, 80);
        assertEquals(ReelJobStage.MUSIC, tracker.get("job-5").stage());
        assertEquals(80, tracker.get("job-5").percent());
    }
}
