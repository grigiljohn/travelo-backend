package com.travelo.notificationservice.event;

import com.travelo.notificationservice.dto.NotificationDto;
import com.travelo.notificationservice.entity.NotificationType;
import com.travelo.notificationservice.service.NotificationBroadcastService;
import com.travelo.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer that turns post / comment / follow / DM events into persisted
 * notifications and fans them out to live WebSocket sessions.
 *
 * <p>Event contract sources:
 * <ul>
 *   <li>{@code post.liked}, {@code comment.created} — {@code social-service}
 *       {@code PostEventPublisher} (see {@code dto.events.PostLikedEvent},
 *       {@code dto.events.CommentCreatedEvent}). Field names are snake_case
 *       (e.g. {@code post_owner_id}, {@code actor_user_id}).</li>
 *   <li>{@code user.followed}, {@code user.unfollowed} — {@code identity-service}
 *       {@code UserEventPublisher}.</li>
 *   <li>{@code dm.received} — emitted by realtime-service messaging flow
 *       (payload still uses the messaging-native keys).</li>
 * </ul>
 *
 * <p>Self-action events (author == actor) are dropped. Unfollow events do not
 * create notifications.
 */
@Component
public class NotificationEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private static final int PREVIEW_LIMIT = 140;

    private final NotificationService notificationService;
    private final NotificationBroadcastService broadcastService;

    public NotificationEventConsumer(NotificationService notificationService,
                                     NotificationBroadcastService broadcastService) {
        this.notificationService = notificationService;
        this.broadcastService = broadcastService;
    }

    /**
     * {@code post.liked} — notify the post owner that someone liked their post.
     * The {@code post.like.removed} topic is intentionally ignored (no notification).
     */
    @KafkaListener(topics = "${app.notifications.topics.post-liked:post.liked}",
            groupId = "${app.notifications.group:notification-service-group}")
    public void handlePostLiked(Map<String, Object> event) {
        try {
            logger.info("flow=notification_received topic=post.liked payload={}", event);
            String action = asString(event.get("action"));
            if ("unliked".equalsIgnoreCase(action)) {
                return; // Do not notify on unlike.
            }
            String postId = firstNonBlank(event, "post_id", "postId");
            String ownerId = firstNonBlank(event, "post_owner_id", "authorId", "ownerId");
            String actorId = firstNonBlank(event, "actor_user_id", "likerId", "actorId");
            if (ownerId == null || actorId == null || postId == null) {
                logger.warn("flow=notification_invalid topic=post.liked payload={}", event);
                return;
            }
            if (ownerId.equals(actorId)) {
                return; // Self-action dedupe.
            }

            Map<String, Object> data = new HashMap<>();
            data.put("post_id", postId);
            data.put("actor_id", actorId);

            NotificationDto dto = notificationService.createNotification(
                    parseUuid(ownerId),
                    NotificationType.POST_LIKED,
                    "New Like",
                    "Someone liked your post",
                    parseUuid(actorId),
                    parseUuid(postId),
                    "POST",
                    data);
            if (dto != null) broadcastService.broadcast(dto);
        } catch (Exception e) {
            logger.error("flow=notification_error topic=post.liked err={}", e.toString(), e);
        }
    }

    /**
     * {@code comment.created} — notify the post owner (and, when it is a reply,
     * switch type to {@link NotificationType#COMMENT_REPLIED} with a reply-aware
     * title).
     */
    @KafkaListener(topics = "${app.notifications.topics.comment-created:comment.created}",
            groupId = "${app.notifications.group:notification-service-group}")
    public void handleCommentCreated(Map<String, Object> event) {
        try {
            logger.info("flow=notification_received topic=comment.created payload={}", event);
            String postId = firstNonBlank(event, "post_id", "postId");
            String commentId = firstNonBlank(event, "comment_id", "commentId");
            String ownerId = firstNonBlank(event, "post_owner_id", "authorId", "ownerId");
            String actorId = firstNonBlank(event, "actor_user_id", "commenterId", "actorId");
            String parentId = firstNonBlank(event, "parent_comment_id", "parentCommentId", "parentId");
            String preview = truncate(firstNonBlankValue(event, "preview", "commentText"));

            if (ownerId == null || actorId == null || postId == null) {
                logger.warn("flow=notification_invalid topic=comment.created payload={}", event);
                return;
            }
            if (ownerId.equals(actorId)) {
                return;
            }

            boolean isReply = parentId != null && !parentId.isBlank();
            NotificationType type = isReply ? NotificationType.COMMENT_REPLIED : NotificationType.POST_COMMENTED;
            String title = isReply ? "New Reply" : "New Comment";

            Map<String, Object> data = new HashMap<>();
            data.put("post_id", postId);
            if (commentId != null) data.put("comment_id", commentId);
            if (parentId != null) data.put("parent_comment_id", parentId);
            data.put("actor_id", actorId);

            NotificationDto dto = notificationService.createNotification(
                    parseUuid(ownerId),
                    type,
                    title,
                    preview == null || preview.isBlank() ? "Someone commented on your post" : preview,
                    parseUuid(actorId),
                    parseUuid(postId),
                    "POST",
                    data);
            if (dto != null) broadcastService.broadcast(dto);
        } catch (Exception e) {
            logger.error("flow=notification_error topic=comment.created err={}", e.toString(), e);
        }
    }

    /**
     * {@code user.followed} — notify the followee; ignore the {@code unfollowed}
     * action here (no "X stopped following you" notification).
     */
    @KafkaListener(topics = "${app.notifications.topics.user-followed:user.followed}",
            groupId = "${app.notifications.group:notification-service-group}")
    public void handleUserFollowed(Map<String, Object> event) {
        try {
            logger.info("flow=notification_received topic=user.followed payload={}", event);
            String action = asString(event.get("action"));
            if ("unfollowed".equalsIgnoreCase(action)) {
                return;
            }
            String followerId = firstNonBlank(event, "follower_id", "followerId");
            String followeeId = firstNonBlank(event, "followee_id", "followedUserId", "followeeId");
            if (followerId == null || followeeId == null) {
                logger.warn("flow=notification_invalid topic=user.followed payload={}", event);
                return;
            }
            if (followerId.equals(followeeId)) {
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("actor_id", followerId);

            NotificationDto dto = notificationService.createNotification(
                    parseUuid(followeeId),
                    NotificationType.USER_FOLLOWED,
                    "New Follower",
                    "Someone started following you",
                    parseUuid(followerId),
                    parseUuid(followerId),
                    "USER",
                    data);
            if (dto != null) broadcastService.broadcast(dto);
        } catch (Exception e) {
            logger.error("flow=notification_error topic=user.followed err={}", e.toString(), e);
        }
    }

    /**
     * {@code dm.received} — legacy messaging event; kept intact for the chat
     * pipeline. Accepts both snake_case and camelCase keys defensively.
     */
    @KafkaListener(topics = "${app.notifications.topics.dm-received:dm.received}",
            groupId = "${app.notifications.group:notification-service-group}")
    public void handleDmReceived(Map<String, Object> event) {
        try {
            logger.info("flow=notification_received topic=dm.received payload={}", event);
            String recipientId = firstNonBlank(event, "recipient_id", "recipientId");
            String senderId = firstNonBlank(event, "sender_id", "senderId");
            String conversationId = firstNonBlank(event, "conversation_id", "conversationId");
            String senderUsername = asString(event.get("senderUsername"));
            String preview = truncate(firstNonBlankValue(event, "messagePreview", "message_preview", "preview"));

            if (recipientId == null || senderId == null) {
                logger.warn("flow=notification_invalid topic=dm.received payload={}", event);
                return;
            }
            if (recipientId.equals(senderId)) {
                return;
            }

            Map<String, Object> data = new HashMap<>();
            if (conversationId != null) data.put("conversation_id", conversationId);
            data.put("actor_id", senderId);

            String body = (senderUsername == null || senderUsername.isBlank()
                    ? "New message"
                    : senderUsername) + (preview == null || preview.isBlank() ? "" : ": " + preview);

            NotificationDto dto = notificationService.createNotification(
                    parseUuid(recipientId),
                    NotificationType.DM_RECEIVED,
                    "New Message",
                    body,
                    parseUuid(senderId),
                    conversationId == null ? null : parseUuid(conversationId),
                    "MESSAGE",
                    data);
            if (dto != null) broadcastService.broadcast(dto);
        } catch (Exception e) {
            logger.error("flow=notification_error topic=dm.received err={}", e.toString(), e);
        }
    }

    // ---------- helpers ----------

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    /**
     * Return the first non-blank string value among {@code keys}, or {@code null}.
     */
    private static String firstNonBlank(Map<String, Object> event, String... keys) {
        String v = firstNonBlankValue(event, keys);
        return v;
    }

    private static String firstNonBlankValue(Map<String, Object> event, String... keys) {
        for (String key : keys) {
            Object raw = event.get(key);
            if (raw == null) continue;
            String s = raw.toString();
            if (!s.isBlank() && !"null".equalsIgnoreCase(s)) {
                return s;
            }
        }
        return null;
    }

    private static UUID parseUuid(String value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String truncate(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.length() <= PREVIEW_LIMIT) return trimmed;
        return trimmed.substring(0, PREVIEW_LIMIT) + "…";
    }
}
