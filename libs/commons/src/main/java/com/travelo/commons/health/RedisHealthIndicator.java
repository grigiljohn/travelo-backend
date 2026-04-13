package com.travelo.commons.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Health indicator for Redis connectivity.
 */
public class RedisHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(RedisHealthIndicator.class);
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisHealthIndicator(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            var connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                return Health.down()
                        .withDetail("redis", "connection factory is null")
                        .build();
            }
            
            String result = connectionFactory.getConnection()
                    .ping();
            
            if ("PONG".equals(result)) {
                return Health.up()
                        .withDetail("redis", "connected")
                        .withDetail("status", "ok")
                        .build();
            } else {
                return Health.down()
                        .withDetail("redis", "unexpected response")
                        .withDetail("response", result)
                        .build();
            }
        } catch (Exception e) {
            logger.error("Redis health check failed", e);
            return Health.down()
                    .withDetail("redis", "connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}

