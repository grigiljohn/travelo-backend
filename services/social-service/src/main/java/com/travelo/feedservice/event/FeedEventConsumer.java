package com.travelo.feedservice.event;

import com.travelo.feedservice.client.UserServiceClient;
import com.travelo.feedservice.service.FeedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer for feed-related events.
 *
 * <p>Listens for:
 * <ul>
 *   <li>{@code post.created} — a new post was published; fan-out by invalidating
 *       each follower's cached feed so the next read recomputes it with the new
 *       item included (push-on-read model).</li>
 *   <li>{@code user.followed} — refresh the follower's feed so the newly followed
 *       author's posts appear on next read.</li>
 *   <li>{@code user.unfollowed} — refresh the follower's feed so the unfollowed
 *       author's posts drop off on next read.</li>
 * </ul>
 *
 * <p>Listeners are started only when {@code spring.kafka.listener.auto-startup=true}
 * (see application.yml). Events may arrive with either snake_case (producer
 * convention) or camelCase (legacy) keys — both are handled.
 */
@Component
public class FeedEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(FeedEventConsumer.class);

    private final FeedService feedService;
    private final UserServiceClient userServiceClient;

    public FeedEventConsumer(FeedService feedService, UserServiceClient userServiceClient) {
        this.feedService = feedService;
        this.userServiceClient = userServiceClient;
    }

    /**
     * Handle post created event.
     * Fan-out: invalidate each follower's cached feed so it recomputes on next read.
     */
    @KafkaListener(
            topics = "${app.post.events.topics.post-created:post.created}",
            groupId = "${app.feed.events.consumer-group:feed-service-group}"
    )
    public void handlePostCreated(Map<String, Object> event) {
        try {
            logger.info("flow=feed_fanout_received payload={}", event);

            UUID postId = readUuid(event, "post_id", "postId");
            UUID authorId = readUuid(event, "author_id", "authorId");

            if (postId == null || authorId == null) {
                logger.warn("flow=feed_fanout_invalid missing postId or authorId in event={}", event);
                return;
            }

            List<UUID> followers = userServiceClient.getFollowers(authorId);
            if (followers.isEmpty()) {
                logger.info("flow=feed_fanout_no_followers authorId={} postId={} (will recompute on next read)",
                        authorId, postId);
                return;
            }

            int touched = 0;
            for (UUID followerId : followers) {
                try {
                    feedService.addPostToFeed(followerId, postId);
                    touched++;
                } catch (Exception ex) {
                    // Isolate per-follower failures — one bad entry must not stop the fan-out.
                    logger.warn("flow=feed_fanout_follower_failed follower={} postId={} err={}",
                            followerId, postId, ex.toString());
                }
            }
            logger.info("flow=feed_fanout_done authorId={} postId={} followers={} touched={}",
                    authorId, postId, followers.size(), touched);

        } catch (Exception e) {
            logger.error("flow=feed_fanout_error err={}", e.getMessage(), e);
        }
    }

    /**
     * Handle user followed event. Refresh the follower's feed so the newly
     * followed author's posts surface on the next read.
     */
    @KafkaListener(
            topics = "${app.feed.events.topics.user-followed:user.followed}",
            groupId = "${app.feed.events.consumer-group:feed-service-group}"
    )
    public void handleUserFollowed(Map<String, Object> event) {
        try {
            logger.info("flow=user_followed_received payload={}", event);
            UUID followerId = readUuid(event, "follower_id", "followerId");
            if (followerId == null) {
                logger.warn("flow=user_followed_invalid missing followerId payload={}", event);
                return;
            }
            feedService.refreshFeed(followerId);
            logger.info("flow=user_followed_refreshed followerId={}", followerId);
        } catch (Exception e) {
            logger.error("flow=user_followed_error err={}", e.getMessage(), e);
        }
    }

    /**
     * Handle user unfollowed event. Refresh feed so unfollowed author drops off.
     */
    @KafkaListener(
            topics = "${app.feed.events.topics.user-unfollowed:user.unfollowed}",
            groupId = "${app.feed.events.consumer-group:feed-service-group}"
    )
    public void handleUserUnfollowed(Map<String, Object> event) {
        try {
            logger.info("flow=user_unfollowed_received payload={}", event);
            UUID followerId = readUuid(event, "follower_id", "followerId");
            if (followerId == null) {
                logger.warn("flow=user_unfollowed_invalid missing followerId payload={}", event);
                return;
            }
            feedService.refreshFeed(followerId);
            logger.info("flow=user_unfollowed_refreshed followerId={}", followerId);
        } catch (Exception e) {
            logger.error("flow=user_unfollowed_error err={}", e.getMessage(), e);
        }
    }

    /** Read a UUID field from the event, trying each of the given keys in order. */
    private static UUID readUuid(Map<String, Object> event, String... keys) {
        for (String k : keys) {
            Object v = event.get(k);
            if (v == null) continue;
            try {
                return UUID.fromString(v.toString());
            } catch (IllegalArgumentException ignored) {
                // try next key
            }
        }
        return null;
    }
}
