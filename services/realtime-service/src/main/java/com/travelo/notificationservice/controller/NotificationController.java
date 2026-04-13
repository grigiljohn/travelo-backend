package com.travelo.notificationservice.controller;

import com.travelo.notificationservice.dto.MarkAsReadRequest;
import com.travelo.notificationservice.dto.NotificationDto;
import com.travelo.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for notifications.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Notification APIs")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Get notifications", 
               description = "Get user's notifications with pagination")
    public ResponseEntity<Map<String, Object>> getNotifications(
            @Parameter(description = "User ID", required = true)
            @RequestParam("user_id") UUID userId,
            @Parameter(description = "Page number", example = "1")
            @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(value = "limit", defaultValue = "20") @Max(100) int limit) {
        
        logger.info("GET /api/v1/notifications - userId={}, page={}, limit={}", userId, page, limit);
        
        List<NotificationDto> notifications = notificationService.getNotifications(userId, page, limit);
        Long unreadCount = notificationService.getUnreadCount(userId);
        
        return ResponseEntity.ok(Map.of(
                "notifications", notifications,
                "unread_count", unreadCount,
                "page", page,
                "limit", limit
        ));
    }

    @PostMapping("/read")
    @Operation(summary = "Mark notifications as read",
               description = "Mark one or more notifications as read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @Valid @RequestBody MarkAsReadRequest request) {
        
        logger.info("POST /api/v1/notifications/read - notificationIds={}", request.getNotificationIds());
        
        notificationService.markAsRead(request.getNotificationIds());
        
        return ResponseEntity.ok(Map.of("message", "Notifications marked as read"));
    }
}

