package com.travelo.commons.idempotency;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for idempotency key support.
 * Requires Redis for storing idempotency responses.
 */
@Configuration
@ConditionalOnClass(RedisTemplate.class)
public class IdempotencyConfig implements WebMvcConfigurer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final long defaultTtlSeconds;

    public IdempotencyConfig(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.defaultTtlSeconds = 86400; // 24 hours
    }

    @Bean
    public IdempotencyKeyInterceptor idempotencyKeyInterceptor() {
        return new IdempotencyKeyInterceptor(redisTemplate, defaultTtlSeconds);
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(idempotencyKeyInterceptor());
    }
}

