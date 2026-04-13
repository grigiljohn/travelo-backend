package com.travelo.feedservice.event;

import com.travelo.feedservice.service.FeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer for feed-related events.
 * Listens for:
 * - post.created: When a new post is created (fan-out to followers)
 * - user.followed: When a user follows another user (update feed)
 * - user.unfollowed: When a user unfollows another user (update feed)
 */
@Component
public class FeedEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(FeedEventConsumer.class);

    private final FeedService feedService;

    public FeedEventConsumer(FeedService feedService) {
        this.feedService = feedService;
    }

    /**
     * Handle post created event.
     * Fan-out: Add post to all followers' feeds.
     */
    //@KafkaListener(topics = "post.created", groupId = "feed-service-group")
    public void handlePostCreated(Map<String, Object> event) {
        try {
            logger.info("Received post.created event: {}", event);
            
            // Extract post ID and author ID
            String postIdStr = (String) event.get("postId");
            String authorIdStr = (String) event.get("authorId");
            
            if (postIdStr == null || authorIdStr == null) {
                logger.warn("Invalid post.created event: missing postId or authorId");
                return;
            }

            UUID postId = UUID.fromString(postIdStr);
            UUID authorId = UUID.fromString(authorIdStr);

            // TODO: Get list of followers from user-service
            // For now, we'll invalidate feeds on next read
            // In a full implementation, you would:
            // 1. Fetch list of followers for authorId
            // 2. For each follower, call feedService.addPostToFeed(followerId, postId)
            
            logger.info("Post {} created by user {}, fan-out will happen on next read", postId, authorId);

        } catch (Exception e) {
            logger.error("Error handling post.created event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle user followed event.
     * Update feed to include posts from newly followed user.
     */
    //@KafkaListener(topics = "user.followed", groupId = "feed-service-group")
    public void handleUserFollowed(Map<String, Object> event) {
        try {
            logger.info("Received user.followed event: {}", event);
            
            String followerIdStr = (String) event.get("followerId");
            
            if (followerIdStr == null) {
                logger.warn("Invalid user.followed event: missing followerId");
                return;
            }

            UUID followerId = UUID.fromString(followerIdStr);
            
            // Refresh feed for the follower
            feedService.refreshFeed(followerId);
            logger.info("Refreshed feed for follower {}", followerId);

        } catch (Exception e) {
            logger.error("Error handling user.followed event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle user unfollowed event.
     * Update feed to remove posts from unfollowed user.
     */
    //@KafkaListener(topics = "user.unfollowed", groupId = "feed-service-group")
    public void handleUserUnfollowed(Map<String, Object> event) {
        try {
            logger.info("Received user.unfollowed event: {}", event);
            
            String followerIdStr = (String) event.get("followerId");
            
            if (followerIdStr == null) {
                logger.warn("Invalid user.unfollowed event: missing followerId");
                return;
            }

            UUID followerId = UUID.fromString(followerIdStr);
            
            // Refresh feed for the follower
            feedService.refreshFeed(followerId);
            logger.info("Refreshed feed for follower {}", followerId);

        } catch (Exception e) {
            logger.error("Error handling user.unfollowed event: {}", e.getMessage(), e);
        }
    }
}

