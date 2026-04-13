package com.travelo.feedservice.service.impl;

import com.travelo.feedservice.service.FeedSeenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based implementation of seen posts tracking.
 * Uses Redis SET for O(1) membership checks via SISMEMBER.
 * 
 * Key format: seen:{userId}:{surface}
 * Value: SET of post IDs (strings)
 * TTL: Configurable (default 7 days)
 */
@Service
public class FeedSeenServiceImpl implements FeedSeenService {

    private static final Logger logger = LoggerFactory.getLogger(FeedSeenServiceImpl.class);
    
    private static final String SEEN_KEY_PREFIX = "seen:";
    private static final int DEFAULT_TTL_DAYS = 7;
    private static final int MAX_TTL_DAYS = 30;
    
    private final RedisTemplate<String, String> redisTemplate;
    private final SetOperations<String, String> setOps;
    private final int ttlDays;
    private final boolean enabled;

    public FeedSeenServiceImpl(
            @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate,
            @Value("${app.feed.seen-ttl-days:" + DEFAULT_TTL_DAYS + "}") int ttlDays,
            @Value("${app.feed.seen-enabled:true}") boolean enabled) {
        this.redisTemplate = redisTemplate;
        this.setOps = redisTemplate.opsForSet();
        this.ttlDays = Math.min(Math.max(ttlDays, 1), MAX_TTL_DAYS); // Clamp between 1 and 30 days
        this.enabled = enabled;
    }

    @Override
    public void markPostsAsSeen(UUID userId, String surface, Set<String> postIds) {
        if (!enabled) {
            logger.debug("Seen tracking disabled, skipping markPostsAsSeen for user {} surface {}", userId, surface);
            return;
        }
        
        if (postIds == null || postIds.isEmpty()) {
            logger.debug("Empty postIds set, skipping markPostsAsSeen");
            return;
        }
        
        String key = getSeenKey(userId, surface);
        
        try {
            // Use SADD to add all post IDs to the set (Redis handles deduplication)
            String[] postIdArray = postIds.toArray(new String[0]);
            Long added = setOps.add(key, postIdArray);
            
            // Set TTL on first add (only if key didn't exist before)
            if (added != null && added > 0) {
                redisTemplate.expire(key, ttlDays, TimeUnit.DAYS);
                logger.debug("Marked {} posts as seen for user {} on surface {} ({} new)", 
                        postIds.size(), userId, surface, added);
            } else {
                // Ensure TTL is set even if no new items were added
                redisTemplate.expire(key, ttlDays, TimeUnit.DAYS);
                logger.debug("Updated seen posts for user {} on surface {} (all already seen)", userId, surface);
            }
            
        } catch (Exception e) {
            // Graceful degradation: log error but don't block
            logger.error("Error marking posts as seen for user {} on surface {}: {}", 
                    userId, surface, e.getMessage(), e);
        }
    }

    @Override
    public Set<String> getSeenPostIds(UUID userId, String surface, Set<String> postIds) {
        if (!enabled) {
            logger.debug("Seen tracking disabled, returning empty set");
            return new HashSet<>();
        }
        
        if (postIds == null || postIds.isEmpty()) {
            return new HashSet<>();
        }
        
        String key = getSeenKey(userId, surface);
        
        try {
            // Use SISMEMBER for each post ID (O(1) per check)
            // Or use SINTER for batch check (more efficient for large sets)
            Set<String> seenPostIds = new HashSet<>();
            
            // SINTER is more efficient for large sets, but we need to check if key exists first
            // For simplicity and O(1) guarantees, we'll use individual SISMEMBER calls
            // In production, you might want to batch these using pipeline for better performance
            for (String postId : postIds) {
                if (Boolean.TRUE.equals(setOps.isMember(key, postId))) {
                    seenPostIds.add(postId);
                }
            }
            
            logger.debug("Found {} seen posts out of {} checked for user {} on surface {}", 
                    seenPostIds.size(), postIds.size(), userId, surface);
            
            return seenPostIds;
            
        } catch (Exception e) {
            // Graceful degradation: log warning and return empty set (treat all as unseen)
            logger.warn("Error checking seen posts for user {} on surface {}: {}. Treating all as unseen.", 
                    userId, surface, e.getMessage());
            return new HashSet<>();
        }
    }

    @Override
    public boolean isPostSeen(UUID userId, String surface, String postId) {
        if (!enabled) {
            return false;
        }
        
        if (postId == null || postId.isEmpty()) {
            return false;
        }
        
        String key = getSeenKey(userId, surface);
        
        try {
            // O(1) operation using SISMEMBER
            Boolean isMember = setOps.isMember(key, postId);
            return Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            // Graceful degradation: return false (treat as unseen)
            logger.warn("Error checking if post {} is seen for user {} on surface {}: {}. Treating as unseen.", 
                    postId, userId, surface, e.getMessage());
            return false;
        }
    }

    @Override
    public void clearSeenPosts(UUID userId, String surface) {
        String key = getSeenKey(userId, surface);
        try {
            redisTemplate.delete(key);
            logger.info("Cleared seen posts for user {} on surface {}", userId, surface);
        } catch (Exception e) {
            logger.error("Error clearing seen posts for user {} on surface {}: {}", 
                    userId, surface, e.getMessage(), e);
        }
    }

    /**
     * Generate Redis key for seen posts.
     * Format: seen:{userId}:{surface}
     */
    private String getSeenKey(UUID userId, String surface) {
        return SEEN_KEY_PREFIX + userId.toString() + ":" + surface;
    }
}

