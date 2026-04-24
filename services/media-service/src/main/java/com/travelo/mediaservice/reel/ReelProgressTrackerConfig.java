package com.travelo.mediaservice.reel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Chooses {@link ReelJobProgressTracker} + {@link ReelJobProgressBroker}
 * implementations at startup based on {@code reel.progress.backend}.
 * Default is {@code memory} for single-node dev; set to {@code redis} in
 * production so progress is shared across media-service replicas and
 * SSE streams can receive events from any replica.
 */
@Configuration
public class ReelProgressTrackerConfig {

    private static final Logger log = LoggerFactory.getLogger(ReelProgressTrackerConfig.class);

    // ---------- memory ----------

    @Bean
    @ConditionalOnProperty(
            name = "reel.progress.backend",
            havingValue = "memory",
            matchIfMissing = true)
    public ReelJobProgressBroker inMemoryReelJobProgressBroker() {
        log.info("reel.progress.backend=memory (single-node broker)");
        return new InMemoryReelJobProgressBroker();
    }

    @Bean
    @ConditionalOnProperty(
            name = "reel.progress.backend",
            havingValue = "memory",
            matchIfMissing = true)
    public ReelJobProgressTracker inMemoryReelJobProgressTracker(ReelJobProgressBroker broker) {
        log.info("reel.progress.backend=memory (single-node in-memory tracker)");
        return new InMemoryReelJobProgressTracker(broker);
    }

    // ---------- redis ----------

    @Bean
    @ConditionalOnProperty(name = "reel.progress.backend", havingValue = "redis")
    public RedisMessageListenerContainer reelProgressRedisListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer c = new RedisMessageListenerContainer();
        c.setConnectionFactory(connectionFactory);
        return c;
    }

    @Bean
    @ConditionalOnProperty(name = "reel.progress.backend", havingValue = "redis")
    public ReelJobProgressBroker redisReelJobProgressBroker(
            StringRedisTemplate redisTemplate,
            RedisMessageListenerContainer container,
            ObjectMapper objectMapper) {
        log.info("reel.progress.backend=redis (Redis Pub/Sub broker)");
        return new RedisReelJobProgressBroker(redisTemplate, container, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "reel.progress.backend", havingValue = "redis")
    public ReelJobProgressTracker redisReelJobProgressTracker(
            StringRedisTemplate redisTemplate,
            ReelJobProgressBroker broker) {
        log.info("reel.progress.backend=redis (hash tracker + Pub/Sub fan-out)");
        return new RedisReelJobProgressTracker(redisTemplate, broker);
    }
}
