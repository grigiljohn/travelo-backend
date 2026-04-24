package com.travelo.postservice.realtime;

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
 * Picks a {@link CommentStreamBroker} implementation at startup based on
 * {@code comments.stream.backend}. Default is {@code memory} for single-node
 * dev; set to {@code redis} in production so that an SSE stream attached to
 * one social-service replica receives comments saved on any replica.
 */
@Configuration
public class CommentStreamConfig {

    private static final Logger log = LoggerFactory.getLogger(CommentStreamConfig.class);

    // ---------- memory ----------

    @Bean
    @ConditionalOnProperty(
            name = "comments.stream.backend",
            havingValue = "memory",
            matchIfMissing = true)
    public CommentStreamBroker inMemoryCommentStreamBroker() {
        log.info("comments.stream.backend=memory (single-node broker)");
        return new InMemoryCommentStreamBroker();
    }

    // ---------- redis ----------

    @Bean("commentStreamRedisListenerContainer")
    @ConditionalOnProperty(name = "comments.stream.backend", havingValue = "redis")
    public RedisMessageListenerContainer commentStreamRedisListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer c = new RedisMessageListenerContainer();
        c.setConnectionFactory(connectionFactory);
        return c;
    }

    @Bean
    @ConditionalOnProperty(name = "comments.stream.backend", havingValue = "redis")
    public CommentStreamBroker redisCommentStreamBroker(
            StringRedisTemplate redisTemplate,
            RedisMessageListenerContainer commentStreamRedisListenerContainer,
            ObjectMapper objectMapper) {
        log.info("comments.stream.backend=redis (Redis Pub/Sub broker)");
        return new RedisCommentStreamBroker(
                redisTemplate,
                commentStreamRedisListenerContainer,
                objectMapper);
    }
}
