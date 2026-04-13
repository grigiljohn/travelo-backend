package com.travelo.notificationservice.service.impl;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import com.travelo.notificationservice.entity.DeviceToken;
import com.travelo.notificationservice.service.PushNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Firebase Cloud Messaging (FCM) implementation for push notifications.
 * Supports both Android and iOS via FCM.
 */
@Service
public class FirebasePushNotificationServiceImpl implements PushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(FirebasePushNotificationServiceImpl.class);

    private final FirebaseMessaging firebaseMessaging;
    private final boolean firebaseEnabled;

    public FirebasePushNotificationServiceImpl() {
        // Initialize Firebase (in production, use service account JSON from config)
        FirebaseMessaging tempMessaging = null;
        boolean enabled = false;
        
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp();
            }
            tempMessaging = FirebaseMessaging.getInstance();
            enabled = true;
            logger.info("Firebase initialized successfully");
        } catch (Exception e) {
            logger.warn("Firebase initialization failed (push notifications will be disabled): {}", e.getMessage());
            logger.debug("Firebase error details", e);
            // Continue without Firebase - service will still start but push notifications won't work
        }
        
        this.firebaseMessaging = tempMessaging;
        this.firebaseEnabled = enabled;
    }

    @Override
    public boolean sendPushNotification(DeviceToken deviceToken, String title, String body, Map<String, Object> data) {
        if (!firebaseEnabled || firebaseMessaging == null) {
            logger.warn("Firebase is not initialized. Push notification not sent.");
            return false;
        }
        
        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(deviceToken.getToken())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            // Add data payload
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> String.valueOf(e.getValue())
                        )));
            }

            // Platform-specific configuration
            if ("IOS".equalsIgnoreCase(deviceToken.getPlatform())) {
                ApnsConfig apnsConfig = ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setSound("default")
                                .setBadge(1)
                                .build())
                        .build();
                messageBuilder.setApnsConfig(apnsConfig);
            } else if ("ANDROID".equalsIgnoreCase(deviceToken.getPlatform())) {
                AndroidConfig androidConfig = AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setSound("default")
                                .build())
                        .build();
                messageBuilder.setAndroidConfig(androidConfig);
            }

            Message message = messageBuilder.build();
            String response = firebaseMessaging.send(message);
            
            logger.info("Successfully sent push notification to device {}: {}", deviceToken.getId(), response);
            return true;
            
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send push notification to device {}: {}", deviceToken.getId(), e.getMessage());
            
            // Handle invalid token
            if (e.getErrorCode().equals("messaging/invalid-registration-token") ||
                e.getErrorCode().equals("messaging/registration-token-not-registered")) {
                logger.warn("Invalid token detected: {}", deviceToken.getToken());
                // Token should be removed from database
            }
            
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error sending push notification: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void sendBatchPushNotifications(List<DeviceToken> deviceTokens, String title, String body, Map<String, Object> data) {
        if (!firebaseEnabled || firebaseMessaging == null) {
            logger.warn("Firebase is not initialized. Batch push notifications not sent.");
            return;
        }
        
        if (deviceTokens == null || deviceTokens.isEmpty()) {
            return;
        }

        List<Message> messages = deviceTokens.stream()
                .map(token -> {
                    Message.Builder builder = Message.builder()
                            .setToken(token.getToken())
                            .setNotification(Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build());

                    if (data != null && !data.isEmpty()) {
                        builder.putAllData(data.entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        e -> String.valueOf(e.getValue())
                                )));
                    }

                    // Platform-specific config
                    if ("IOS".equalsIgnoreCase(token.getPlatform())) {
                        builder.setApnsConfig(ApnsConfig.builder()
                                .setAps(Aps.builder().setSound("default").setBadge(1).build())
                                .build());
                    } else if ("ANDROID".equalsIgnoreCase(token.getPlatform())) {
                        builder.setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .setNotification(AndroidNotification.builder().setSound("default").build())
                                .build());
                    }

                    return builder.build();
                })
                .collect(Collectors.toList());

        try {
            BatchResponse batchResponse = firebaseMessaging.sendAll(messages);
            logger.info("Batch push notification sent. Success: {}, Failure: {}", 
                    batchResponse.getSuccessCount(), batchResponse.getFailureCount());

            // Handle failed tokens
            if (batchResponse.getFailureCount() > 0) {
                batchResponse.getResponses().forEach(response -> {
                    if (!response.isSuccessful()) {
                        logger.warn("Failed to send batch notification: {}", response.getException().getMessage());
                    }
                });
            }
        } catch (Exception e) {
            logger.error("Error sending batch push notifications: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean validateToken(DeviceToken deviceToken) {
        if (!firebaseEnabled || firebaseMessaging == null) {
            logger.warn("Firebase is not initialized. Token validation skipped.");
            return false;
        }
        
        // Send a test message or use FCM token validation API
        try {
            // FCM doesn't have a direct validation API, so we can try sending a silent notification
            Message testMessage = Message.builder()
                    .setToken(deviceToken.getToken())
                    .putData("test", "true")
                    .build();
            firebaseMessaging.send(testMessage);
            return true;
        } catch (FirebaseMessagingException e) {
            if (e.getErrorCode().equals("messaging/invalid-registration-token") ||
                e.getErrorCode().equals("messaging/registration-token-not-registered")) {
                return false;
            }
            logger.warn("Token validation failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }
}

