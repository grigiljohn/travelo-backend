package com.travelo.feedservice.service.impl;

import com.travelo.feedservice.service.FeedMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;

@Service
public class FeedMetricsServiceImpl implements FeedMetricsService {

    private final MeterRegistry meterRegistry;

    public FeedMetricsServiceImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public <T> T timeGetFeed(String surface, Callable<T> callable) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return callable.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            sample.stop(Timer.builder("feed.get.duration")
                    .tag("surface", safe(surface))
                    .register(meterRegistry));
        }
    }

    @Override
    public void recordFeedServed(String surface, int totalItems, int postOrReelItems, int adItems) {
        Counter.builder("feed.served.count")
                .tag("surface", safe(surface))
                .register(meterRegistry)
                .increment();
        Counter.builder("feed.served.items")
                .tag("surface", safe(surface))
                .tag("type", "total")
                .register(meterRegistry)
                .increment(Math.max(0, totalItems));
        Counter.builder("feed.served.items")
                .tag("surface", safe(surface))
                .tag("type", "content")
                .register(meterRegistry)
                .increment(Math.max(0, postOrReelItems));
        Counter.builder("feed.served.items")
                .tag("surface", safe(surface))
                .tag("type", "ads")
                .register(meterRegistry)
                .increment(Math.max(0, adItems));
    }

    @Override
    public void recordFallbackUsed(String dependency, String mode) {
        Counter.builder("feed.fallback.used")
                .tag("dependency", safe(dependency))
                .tag("mode", safe(mode))
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void recordOnlineSignalEvent(String eventType) {
        Counter.builder("feed.online_signal.event")
                .tag("event_type", safe(eventType))
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void recordFatigueReorder(String surface, int reorderedCount) {
        Counter.builder("feed.fatigue.reordered")
                .tag("surface", safe(surface))
                .register(meterRegistry)
                .increment(Math.max(0, reorderedCount));
    }

    @Override
    public void recordSeenSuppressed(String surface, String mode, int count) {
        if (count <= 0) {
            return;
        }
        Counter.builder("feed.seen.suppressed")
                .tag("surface", safe(surface))
                .tag("mode", safe(mode))
                .register(meterRegistry)
                .increment(count);
    }

    private String safe(String v) {
        if (v == null || v.isBlank()) {
            return "unknown";
        }
        return v.trim().toLowerCase();
    }
}

