package com.travelo.notificationservice.service;

import com.travelo.notificationservice.dto.NotificationDto;
import com.travelo.notificationservice.entity.NotificationType;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing notifications.
 */
public interface NotificationService {
    
    /**
     * Create and send a notification.
     */
    NotificationDto createNotification(
            UUID userId,
            NotificationType type,
            String title,
            String body,
            UUID actorId,
            UUID targetId,
            String targetType,
            java.util.Map<String, Object> data);
    
    /**
     * Get user's notifications.
     */
    List<NotificationDto> getNotifications(UUID userId, int page, int limit);
    
    /**
     * Mark notifications as read.
     */
    void markAsRead(List<UUID> notificationIds);
    
    /**
     * Get unread count.
     */
    Long getUnreadCount(UUID userId);
}

