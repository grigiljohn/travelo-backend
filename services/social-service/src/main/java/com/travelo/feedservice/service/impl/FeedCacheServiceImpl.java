package com.travelo.feedservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelo.feedservice.dto.FeedItem;
import com.travelo.feedservice.service.FeedCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Redis-based feed cache implementation using sorted sets.
 * Each user's feed is stored as a sorted set with score = ranking score.
 */
@Service
public class FeedCacheServiceImpl implements FeedCacheService {

    private static final Logger logger = LoggerFactory.getLogger(FeedCacheServiceImpl.class);
    
    private static final String FEED_KEY_PREFIX = "feed:user:";
    private static final int DEFAULT_EXPIRY_SECONDS = 3600; // 1 hour

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ZSetOperations<String, String> zSetOps;

    public FeedCacheServiceImpl(@Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.zSetOps = redisTemplate.opsForZSet();
    }

    @Override
    public List<FeedItem> getCachedFeed(UUID userId, String cursor, int limit) {
        String key = getFeedKey(userId);
        
        try {
            // Parse cursor (score from last item)
            double minScore = Double.NEGATIVE_INFINITY;
            if (cursor != null && !cursor.isEmpty()) {
                try {
                    minScore = Double.parseDouble(cursor);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid cursor format: {}", cursor);
                }
            }

            // Get items with score < cursor (descending order, highest score first)
            // Redis ZREVRANGEBYSCORE gets items in descending score order
            Set<String> items = zSetOps.reverseRangeByScore(key, Double.POSITIVE_INFINITY, minScore, 0, limit);
            
            if (items == null || items.isEmpty()) {
                return new ArrayList<>();
            }

            // Deserialize feed items
            List<FeedItem> feedItems = new ArrayList<>();
            for (String itemJson : items) {
                try {
                    FeedItem item = objectMapper.readValue(itemJson, FeedItem.class);
                    feedItems.add(item);
                } catch (Exception e) {
                    logger.error("Error deserializing feed item: {}", e.getMessage());
                }
            }

            logger.debug("Retrieved {} feed items from cache for user {}", feedItems.size(), userId);
            return feedItems;

        } catch (Exception e) {
            logger.error("Error retrieving cached feed for user {}: {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void cacheFeed(UUID userId, List<FeedItem> feedItems) {
        String key = getFeedKey(userId);
        
        try {
            // Clear existing feed
            redisTemplate.delete(key);

            // Add items to sorted set with scores
            // Use negative index as score to maintain order (highest rank = highest score)
            // We'll use a descending index (larger index = higher score)
            for (int i = 0; i < feedItems.size(); i++) {
                FeedItem item = feedItems.get(i);
                double score = feedItems.size() - i; // Higher rank = higher score
                
                try {
                    String itemJson = objectMapper.writeValueAsString(item);
                    zSetOps.add(key, itemJson, score);
                } catch (Exception e) {
                    logger.error("Error serializing feed item: {}", e.getMessage());
                }
            }

            // Set expiry
            redisTemplate.expire(key, DEFAULT_EXPIRY_SECONDS, java.util.concurrent.TimeUnit.SECONDS);

            logger.info("Cached {} feed items for user {}", feedItems.size(), userId);

        } catch (Exception e) {
            logger.error("Error caching feed for user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    public void invalidateFeed(UUID userId) {
        String key = getFeedKey(userId);
        try {
            redisTemplate.delete(key);
            logger.info("Invalidated feed cache for user {}", userId);
        } catch (Exception e) {
            logger.error("Error invalidating feed cache for user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    public boolean isFeedCached(UUID userId) {
        String key = getFeedKey(userId);
        try {
            Long count = zSetOps.count(key, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("Error checking feed cache for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    @Override
    public long getCachedFeedCount(UUID userId) {
        String key = getFeedKey(userId);
        try {
            Long count = zSetOps.count(key, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            return count != null ? count : 0;
        } catch (Exception e) {
            logger.error("Error getting feed count for user {}: {}", userId, e.getMessage());
            return 0;
        }
    }

    private String getFeedKey(UUID userId) {
        return FEED_KEY_PREFIX + userId.toString();
    }
}

