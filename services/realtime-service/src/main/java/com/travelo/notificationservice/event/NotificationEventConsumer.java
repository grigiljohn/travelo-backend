package com.travelo.notificationservice.event;

import com.travelo.notificationservice.entity.NotificationType;
import com.travelo.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer for notification events.
 * Consumes: post.liked, comment.created, user.followed, dm.received
 */
@Component
public class NotificationEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final NotificationService notificationService;

    public NotificationEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "post.liked", groupId = "notification-service-group")
    public void handlePostLiked(Map<String, Object> event) {
        try {
            logger.info("Received post.liked event: {}", event);
            
            String postId = String.valueOf(event.get("postId"));
            String authorId = String.valueOf(event.get("authorId"));
            String likerId = String.valueOf(event.get("likerId"));
            String likerUsername = (String) event.get("likerUsername");

            // Don't notify if user liked their own post
            if (authorId.equals(likerId)) {
                return;
            }

            notificationService.createNotification(
                    UUID.fromString(authorId),
                    NotificationType.POST_LIKED,
                    "New Like",
                    likerUsername + " liked your post",
                    UUID.fromString(likerId),
                    UUID.fromString(postId),
                    "POST",
                    Map.of("postId", postId)
            );
        } catch (Exception e) {
            logger.error("Error handling post.liked event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "comment.created", groupId = "notification-service-group")
    public void handleCommentCreated(Map<String, Object> event) {
        try {
            logger.info("Received comment.created event: {}", event);
            
            String postId = String.valueOf(event.get("postId"));
            String commentId = String.valueOf(event.get("commentId"));
            String authorId = String.valueOf(event.get("authorId"));
            String commenterId = String.valueOf(event.get("commenterId"));
            String commenterUsername = (String) event.get("commenterUsername");
            String commentText = (String) event.getOrDefault("commentText", "");

            // Don't notify if user commented on their own post
            if (authorId.equals(commenterId)) {
                return;
            }

            notificationService.createNotification(
                    UUID.fromString(authorId),
                    NotificationType.POST_COMMENTED,
                    "New Comment",
                    commenterUsername + " commented: " + (commentText.length() > 50 ? commentText.substring(0, 50) + "..." : commentText),
                    UUID.fromString(commenterId),
                    UUID.fromString(postId),
                    "POST",
                    Map.of("postId", postId, "commentId", commentId)
            );
        } catch (Exception e) {
            logger.error("Error handling comment.created event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "user.followed", groupId = "notification-service-group")
    public void handleUserFollowed(Map<String, Object> event) {
        try {
            logger.info("Received user.followed event: {}", event);
            
            String followedUserId = String.valueOf(event.get("followedUserId"));
            String followerId = String.valueOf(event.get("followerId"));
            String followerUsername = (String) event.get("followerUsername");

            notificationService.createNotification(
                    UUID.fromString(followedUserId),
                    NotificationType.USER_FOLLOWED,
                    "New Follower",
                    followerUsername + " started following you",
                    UUID.fromString(followerId),
                    UUID.fromString(followerId),
                    "USER",
                    Map.of("followerId", followerId)
            );
        } catch (Exception e) {
            logger.error("Error handling user.followed event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "dm.received", groupId = "notification-service-group")
    public void handleDmReceived(Map<String, Object> event) {
        try {
            logger.info("Received dm.received event: {}", event);
            
            String recipientId = String.valueOf(event.get("recipientId"));
            String senderId = String.valueOf(event.get("senderId"));
            String senderUsername = (String) event.get("senderUsername");
            String messagePreview = (String) event.getOrDefault("messagePreview", "");

            notificationService.createNotification(
                    UUID.fromString(recipientId),
                    NotificationType.DM_RECEIVED,
                    "New Message",
                    senderUsername + ": " + messagePreview,
                    UUID.fromString(senderId),
                    UUID.fromString(String.valueOf(event.get("conversationId"))),
                    "MESSAGE",
                    Map.of("conversationId", event.get("conversationId"))
            );
        } catch (Exception e) {
            logger.error("Error handling dm.received event: {}", e.getMessage(), e);
        }
    }
}

