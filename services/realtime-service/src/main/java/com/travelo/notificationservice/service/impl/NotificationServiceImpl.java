package com.travelo.notificationservice.service.impl;

import com.travelo.notificationservice.dto.NotificationDto;
import com.travelo.notificationservice.entity.*;
import com.travelo.notificationservice.repository.*;
import com.travelo.notificationservice.service.NotificationService;
import com.travelo.notificationservice.service.PushNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(transactionManager = "notificationTransactionManager")
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final PushNotificationService pushNotificationService;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationPreferenceRepository preferenceRepository,
            DeviceTokenRepository deviceTokenRepository,
            PushNotificationService pushNotificationService) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.pushNotificationService = pushNotificationService;
    }

    @Override
    @Transactional(transactionManager = "notificationTransactionManager")
    public NotificationDto createNotification(
            UUID userId,
            NotificationType type,
            String title,
            String body,
            UUID actorId,
            UUID targetId,
            String targetType,
            Map<String, Object> data) {

        // Check user preferences
        NotificationPreference preference = preferenceRepository
                .findByUserIdAndNotificationType(userId, type)
                .orElse(null);

        // If preference exists and disabled, don't create notification
        if (preference != null && Boolean.FALSE.equals(preference.getEnabled())) {
            logger.debug("Notification {} disabled for user {}", type, userId);
            return null;
        }

        // Create notification
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setActorId(actorId);
        notification.setTargetId(targetId);
        notification.setTargetType(targetType);
        notification.setData(data);
        notification.setInAppDelivered(true);

        notification = notificationRepository.save(notification);

        // Send push notification if enabled
        if (preference == null || Boolean.TRUE.equals(preference.getPushEnabled())) {
            sendPushNotification(notification, preference);
        }

        logger.info("Created notification {} for user {}", notification.getId(), userId);
        return toDto(notification);
    }

    private void sendPushNotification(Notification notification, NotificationPreference preference) {
        try {
            List<DeviceToken> deviceTokens = deviceTokenRepository.findByUserId(notification.getUserId());
            
            if (deviceTokens.isEmpty()) {
                logger.debug("No device tokens found for user {}", notification.getUserId());
                return;
            }

            // Prepare data payload
            Map<String, Object> data = new java.util.HashMap<>();
            if (notification.getData() != null) {
                data.putAll(notification.getData());
            }
            data.put("notificationId", notification.getId().toString());
            data.put("type", notification.getNotificationType().name());
            if (notification.getTargetId() != null) {
                data.put("targetId", notification.getTargetId().toString());
                data.put("targetType", notification.getTargetType());
            }

            // Send batch push
            pushNotificationService.sendBatchPushNotifications(
                    deviceTokens,
                    notification.getTitle(),
                    notification.getBody(),
                    data
            );

            notification.setPushed(true);
            notification.setPushedAt(OffsetDateTime.now());
            notificationRepository.save(notification);

        } catch (Exception e) {
            logger.error("Error sending push notification: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<NotificationDto> getNotifications(UUID userId, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        return notifications.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(transactionManager = "notificationTransactionManager")
    public void markAsRead(List<UUID> notificationIds) {
        notificationIds.forEach(id -> {
            notificationRepository.findById(id).ifPresent(notification -> {
                notification.setRead(true);
                notification.setReadAt(OffsetDateTime.now());
                notificationRepository.save(notification);
            });
        });
    }

    @Override
    public Long getUnreadCount(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    private NotificationDto toDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setType(notification.getNotificationType().name());
        dto.setTitle(notification.getTitle());
        dto.setBody(notification.getBody());
        dto.setData(notification.getData());
        dto.setActorId(notification.getActorId());
        dto.setTargetId(notification.getTargetId());
        dto.setTargetType(notification.getTargetType());
        dto.setRead(notification.getRead());
        dto.setReadAt(notification.getReadAt());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }
}

