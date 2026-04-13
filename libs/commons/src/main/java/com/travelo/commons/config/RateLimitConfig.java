package com.travelo.commons.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting configuration using Bucket4j.
 * Provides in-memory token bucket algorithm for rate limiting.
 */
@Configuration
public class RateLimitConfig {

    /**
     * Default rate limit: 100 requests per minute per user.
     */
    public static final int DEFAULT_RATE_LIMIT = 100;
    public static final Duration DEFAULT_DURATION = Duration.ofMinutes(1);

    @Bean
    public static Map<String, Bucket> rateLimitBuckets() {
        return new ConcurrentHashMap<>();
    }

    /**
     * Create or get a bucket for a given key.
     */
    public static Bucket getBucket(Map<String, Bucket> buckets, String key, int capacity, Duration duration) {
        return buckets.computeIfAbsent(key, k -> {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(capacity)
                    .refillGreedy(capacity, duration)
                    .build();
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        });
    }

    /**
     * Create a bucket with default settings.
     */
    public static Bucket getDefaultBucket(Map<String, Bucket> buckets, String key) {
        return getBucket(buckets, key, DEFAULT_RATE_LIMIT, DEFAULT_DURATION);
    }
}

