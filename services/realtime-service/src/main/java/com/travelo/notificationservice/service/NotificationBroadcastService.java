package com.travelo.notificationservice.service;

import com.travelo.notificationservice.dto.NotificationDto;
import com.travelo.notificationservice.repository.NotificationRepository;
import com.travelo.websocketservice.handler.ChatWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pushes new notifications to the recipient's WebSocket sessions via the
 * existing {@link ChatWebSocketHandler}. When the user is offline the
 * notification is still persisted (via {@link NotificationService}); this
 * service only handles live fan-out.
 *
 * <p>Emits two events to the recipient:
 * <ul>
 *   <li>{@code notification.created} — single new notification payload mirroring
 *       the REST {@link NotificationDto} shape.</li>
 *   <li>{@code notification.unread_count} — updated unread count so badges
 *       update instantly without requiring a refetch.</li>
 * </ul>
 */
@Service
public class NotificationBroadcastService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationBroadcastService.class);

    private final ChatWebSocketHandler webSocketHandler;
    private final NotificationRepository notificationRepository;

    public NotificationBroadcastService(ChatWebSocketHandler webSocketHandler,
                                        NotificationRepository notificationRepository) {
        this.webSocketHandler = webSocketHandler;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Deliver the notification to the recipient's live sessions (no-op if they
     * are not connected). Always safe to call — failures are swallowed so the
     * persistence path never breaks on delivery issues.
     */
    public void broadcast(NotificationDto notification) {
        if (notification == null || notification.getUserId() == null) {
            return;
        }
        UUID userId = notification.getUserId();
        try {
            webSocketHandler.sendToUser(userId, eventFrame("notification.created", toWireMap(notification)));
            Long unread = safeUnreadCount(userId);
            if (unread != null) {
                webSocketHandler.sendToUser(userId, eventFrame("notification.unread_count", Map.of(
                        "user_id", userId.toString(),
                        "unread_count", unread
                )));
            }
        } catch (Exception ex) {
            logger.warn("flow=notification_broadcast_failed userId={} err={}", userId, ex.toString());
        }
    }

    private Long safeUnreadCount(UUID userId) {
        try {
            return notificationRepository.countUnreadByUserId(userId);
        } catch (Exception ex) {
            logger.debug("flow=notification_unread_count_failed userId={} err={}", userId, ex.toString());
            return null;
        }
    }

    private static Map<String, Object> eventFrame(String type, Object payload) {
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("type", type);
        frame.put("payload", payload);
        frame.put("timestamp", Instant.now().toString());
        return frame;
    }

    /**
     * Mirror the REST shape so mobile clients can reuse the same parser for
     * WebSocket-delivered and HTTP-fetched notifications.
     */
    private static Map<String, Object> toWireMap(NotificationDto dto) {
        Map<String, Object> out = new HashMap<>();
        out.put("id", dto.getId() == null ? null : dto.getId().toString());
        out.put("user_id", dto.getUserId() == null ? null : dto.getUserId().toString());
        out.put("type", dto.getType());
        out.put("title", dto.getTitle());
        out.put("body", dto.getBody());
        out.put("data", dto.getData());
        out.put("actor_id", dto.getActorId() == null ? null : dto.getActorId().toString());
        out.put("target_id", dto.getTargetId() == null ? null : dto.getTargetId().toString());
        out.put("target_type", dto.getTargetType());
        out.put("read", Boolean.TRUE.equals(dto.getRead()));
        out.put("created_at", dto.getCreatedAt() == null ? null : dto.getCreatedAt().toString());
        return out;
    }
}
