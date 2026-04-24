package com.travelo.mediaservice.reel;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryReelJobProgressBrokerTest {

    private static ReelJobProgressTracker.Snapshot snap(ReelJobStage stage, int pct) {
        return new ReelJobProgressTracker.Snapshot(stage, null, pct, Instant.now());
    }

    @Test
    void publishDeliversToAllSubscribersOfSameJob() {
        InMemoryReelJobProgressBroker broker = new InMemoryReelJobProgressBroker();
        List<ReelJobProgressTracker.Snapshot> a = new ArrayList<>();
        List<ReelJobProgressTracker.Snapshot> b = new ArrayList<>();
        broker.subscribe("j1", a::add);
        broker.subscribe("j1", b::add);

        broker.publish("j1", snap(ReelJobStage.FILTERING, 50));
        broker.publish("j1", snap(ReelJobStage.MUSIC, 80));

        assertEquals(2, a.size());
        assertEquals(2, b.size());
        assertEquals(ReelJobStage.MUSIC, a.get(1).stage());
    }

    @Test
    void subscribersIsolatedPerJobId() {
        InMemoryReelJobProgressBroker broker = new InMemoryReelJobProgressBroker();
        List<ReelJobProgressTracker.Snapshot> a = new ArrayList<>();
        List<ReelJobProgressTracker.Snapshot> b = new ArrayList<>();
        broker.subscribe("j1", a::add);
        broker.subscribe("j2", b::add);

        broker.publish("j1", snap(ReelJobStage.READY, 100));

        assertEquals(1, a.size());
        assertTrue(b.isEmpty());
    }

    @Test
    void closingSubscriptionStopsDelivery() {
        InMemoryReelJobProgressBroker broker = new InMemoryReelJobProgressBroker();
        List<ReelJobProgressTracker.Snapshot> a = new ArrayList<>();
        ReelJobProgressBroker.Subscription sub = broker.subscribe("j1", a::add);

        broker.publish("j1", snap(ReelJobStage.OPTIMIZING, 15));
        sub.close();
        broker.publish("j1", snap(ReelJobStage.FILTERING, 50));

        assertEquals(1, a.size());
        assertEquals(ReelJobStage.OPTIMIZING, a.get(0).stage());
    }

    @Test
    void misbehavingListenerDoesNotBreakOthers() {
        InMemoryReelJobProgressBroker broker = new InMemoryReelJobProgressBroker();
        List<ReelJobProgressTracker.Snapshot> good = new ArrayList<>();
        broker.subscribe("j1", s -> { throw new RuntimeException("boom"); });
        broker.subscribe("j1", good::add);

        broker.publish("j1", snap(ReelJobStage.FILTERING, 40));

        assertEquals(1, good.size());
    }

    @Test
    void inMemoryTrackerPublishesSnapshotToBroker() {
        InMemoryReelJobProgressBroker broker = new InMemoryReelJobProgressBroker();
        InMemoryReelJobProgressTracker tracker = new InMemoryReelJobProgressTracker(broker);
        List<ReelJobProgressTracker.Snapshot> events = new ArrayList<>();
        broker.subscribe("jobA", events::add);

        tracker.report("jobA", ReelJobStage.FILTERING, "encode", 42);
        tracker.report("jobA", ReelJobStage.MUSIC, null, 80);

        assertEquals(2, events.size());
        assertEquals(ReelJobStage.FILTERING, events.get(0).stage());
        assertEquals(42, events.get(0).percent());
        assertEquals(ReelJobStage.MUSIC, events.get(1).stage());
        assertEquals(80, events.get(1).percent());
    }

    @Test
    void blankJobIdOrNullListenerReturnsNoopSubscription() {
        InMemoryReelJobProgressBroker broker = new InMemoryReelJobProgressBroker();
        ReelJobProgressBroker.Subscription s1 = broker.subscribe("", sn -> {});
        ReelJobProgressBroker.Subscription s2 = broker.subscribe("j", null);

        s1.close();
        s2.close();

        broker.publish("j", snap(ReelJobStage.READY, 100));
    }
}
