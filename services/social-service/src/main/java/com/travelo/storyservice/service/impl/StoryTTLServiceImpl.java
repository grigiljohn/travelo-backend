package com.travelo.storyservice.service.impl;

import com.travelo.storyservice.service.StoryTTLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class StoryTTLServiceImpl implements StoryTTLService {

    private static final Logger logger = LoggerFactory.getLogger(StoryTTLServiceImpl.class);
    private static final String TTL_KEY_PREFIX = "story:ttl:";
    private static final long DEFAULT_TTL_SECONDS = 24 * 60 * 60; // 24 hours

    private final RedisTemplate<String, String> redisTemplate;

    public StoryTTLServiceImpl(@Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void setStoryTTL(UUID storyId, long ttlSeconds) {
        String key = getTTLKey(storyId);
        try {
            redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
            logger.debug("Set TTL for story {}: {} seconds", storyId, ttlSeconds);
        } catch (Exception e) {
            logger.error("Error setting TTL for story {}: {}", storyId, e.getMessage());
        }
    }

    @Override
    public boolean isStoryActive(UUID storyId) {
        String key = getTTLKey(storyId);
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            logger.error("Error checking story TTL for {}: {}", storyId, e.getMessage());
            // If Redis is down, assume story is active (fallback to DB)
            return true;
        }
    }

    @Override
    public void removeStoryTTL(UUID storyId) {
        String key = getTTLKey(storyId);
        try {
            redisTemplate.delete(key);
            logger.debug("Removed TTL for story {}", storyId);
        } catch (Exception e) {
            logger.error("Error removing TTL for story {}: {}", storyId, e.getMessage());
        }
    }

    @Override
    public long getRemainingTTL(UUID storyId) {
        String key = getTTLKey(storyId);
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null ? ttl : 0;
        } catch (Exception e) {
            logger.error("Error getting TTL for story {}: {}", storyId, e.getMessage());
            return 0;
        }
    }

    private String getTTLKey(UUID storyId) {
        return TTL_KEY_PREFIX + storyId.toString();
    }
}

