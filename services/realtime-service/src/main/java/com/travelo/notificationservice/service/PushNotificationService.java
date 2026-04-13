package com.travelo.notificationservice.service;

import com.travelo.notificationservice.entity.DeviceToken;

/**
 * Service for sending push notifications via FCM/APNS.
 */
public interface PushNotificationService {
    
    /**
     * Send push notification to a device token.
     */
    boolean sendPushNotification(DeviceToken deviceToken, String title, String body, java.util.Map<String, Object> data);
    
    /**
     * Send batch push notifications.
     */
    void sendBatchPushNotifications(java.util.List<DeviceToken> deviceTokens, String title, String body, java.util.Map<String, Object> data);
    
    /**
     * Validate device token.
     */
    boolean validateToken(DeviceToken deviceToken);
}

