package com.travelo.messagingservice.controller;

import com.travelo.commons.security.SecurityUtils;
import com.travelo.messagingservice.dto.*;
import com.travelo.messagingservice.service.MessagingService;
import com.travelo.realtimeservice.config.LoopbackHttp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Messaging", description = "Direct messaging and group chat APIs")
public class MessagingController {

    private static final Logger logger = LoggerFactory.getLogger(MessagingController.class);

    private final MessagingService messagingService;

    public MessagingController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @PostMapping("/conversations")
    @Operation(summary = "Create conversation", description = "Create a direct or group conversation")
    public ResponseEntity<ConversationDto> createConversation(@Valid @RequestBody CreateConversationRequest request) {
        logger.info("POST /api/v1/conversations");
        return ResponseEntity.ok(messagingService.createConversation(request));
    }

    @GetMapping("/conversations")
    @Operation(summary = "Get conversations", description = "Get user's conversations")
    public ResponseEntity<List<ConversationDto>> getConversations(
            @RequestParam("user_id") UUID userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") @Max(100) int limit) {
        logger.info("GET /api/v1/conversations - userId={}, page={}, limit={}", userId, page, limit);
        try {
            List<ConversationDto> conversations = messagingService.getConversations(userId, page, limit);
            logger.info("GET /api/v1/conversations - Returning {} conversations for userId={}", conversations.size(), userId);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            logger.error("Error in getConversations for userId={}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Compatibility alias: same as GET /conversations?user_id=… (JWT user must match path user).
     */
    @GetMapping("/conversations/user/{userId}")
    @Operation(summary = "List conversations for user (path)", description = "Alias of GET /conversations?user_id=")
    public ResponseEntity<List<ConversationDto>> getConversationsForUserPath(
            @PathVariable("userId") UUID userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") @Max(100) int limit) {
        UUID jwtUser = SecurityUtils.getCurrentUserId();
        if (jwtUser == null || !jwtUser.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can only list your own conversations");
        }
        return ResponseEntity.ok(messagingService.getConversations(userId, page, limit));
    }

    @GetMapping("/conversations/{id}/messages")
    @Operation(summary = "Get messages", description = "Get messages in a conversation")
    public ResponseEntity<List<MessageDto>> getMessages(
            @PathVariable("id") UUID conversationId,
            @RequestParam("user_id") UUID userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "50") @Max(100) int limit) {
        logger.info("GET /api/v1/conversations/{}/messages", conversationId);
        return ResponseEntity.ok(messagingService.getMessages(conversationId, userId, page, limit));
    }

    @GetMapping("/conversations/{conversationId}/messages/{messageId}")
    @Operation(summary = "Get one message", description = "Fetch a single message (participants, or loopback without user_id for internal broadcast)")
    public ResponseEntity<MessageDto> getOneMessage(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            @RequestParam(value = "user_id", required = false) UUID userId,
            HttpServletRequest httpRequest) {
        if (LoopbackHttp.isLoopback(httpRequest)) {
            return ResponseEntity.ok(messagingService.getMessage(conversationId, messageId, null));
        }
        UUID uid = userId != null ? userId : SecurityUtils.getCurrentUserId();
        if (uid == null) {
            throw new IllegalArgumentException("user_id or authentication required");
        }
        return ResponseEntity.ok(messagingService.getMessage(conversationId, messageId, uid));
    }

    @PostMapping("/conversations/{id}/messages")
    @Operation(summary = "Send message", description = "Send a message in a conversation")
    public ResponseEntity<MessageDto> sendMessage(
            @PathVariable("id") UUID conversationId,
            @Valid @RequestBody CreateMessageRequest request,
            HttpServletRequest httpRequest) {
        logger.info("POST /api/v1/conversations/{}/messages - Request received", conversationId);
        logger.debug("Request body: conversationId={}, content length={}, messageType={}", 
                conversationId, 
                request.getContent() != null ? request.getContent().length() : 0,
                request.getMessageType());
        
        try {
            // Extract sender_id from JWT token for security; loopback internal calls may pass sender_id in body
            UUID senderId = SecurityUtils.getCurrentUserId();
            if (senderId == null && LoopbackHttp.isLoopback(httpRequest) && request.getSenderId() != null) {
                senderId = request.getSenderId();
                logger.info("Loopback internal send: using sender_id from body ({})", senderId);
            }
            logger.debug("Extracted senderId from JWT: {}", senderId);
            
            if (senderId == null) {
                logger.error("User not authenticated - senderId is null");
                throw new IllegalArgumentException("User not authenticated");
            }
            
            request.setConversationId(conversationId);
            request.setSenderId(senderId); // Set from JWT, not from request body
            logger.debug("Set request: conversationId={}, senderId={}", conversationId, senderId);
            
            MessageDto result = messagingService.sendMessage(request);
            logger.info("Message sent successfully - messageId={}, conversationId={}", 
                    result.getId(), conversationId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error sending message - conversationId={}, error={}", 
                    conversationId, e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/conversations/{id}/read")
    @Operation(summary = "Mark as read", description = "Mark all messages in conversation as read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable("id") UUID conversationId,
            @RequestParam("user_id") UUID userId) {
        logger.info("POST /api/v1/conversations/{}/read", conversationId);
        messagingService.markAsRead(conversationId, userId);
        return ResponseEntity.ok(Map.of("message", "Messages marked as read"));
    }

    @GetMapping("/conversations/{id}")
    @Operation(summary = "Get conversation info", description = "Get conversation details")
    public ResponseEntity<ConversationDto> getConversationInfo(
            @PathVariable("id") UUID conversationId,
            @RequestParam("user_id") UUID userId) {
        logger.info("GET /api/v1/conversations/{}", conversationId);
        return ResponseEntity.ok(messagingService.getConversationInfo(conversationId, userId));
    }

    @DeleteMapping("/conversations/{id}")
    @Operation(summary = "Delete conversation", description = "Delete a conversation")
    public ResponseEntity<Map<String, String>> deleteConversation(
            @PathVariable("id") UUID conversationId,
            @RequestParam("user_id") UUID userId) {
        logger.info("DELETE /api/v1/conversations/{}", conversationId);
        messagingService.deleteConversation(conversationId, userId);
        return ResponseEntity.ok(Map.of("message", "Conversation deleted successfully"));
    }

    @DeleteMapping("/conversations/{id}/messages/{messageId}")
    @Operation(summary = "Delete message", description = "Delete a message from conversation")
    public ResponseEntity<Map<String, String>> deleteMessage(
            @PathVariable("id") UUID conversationId,
            @PathVariable("messageId") UUID messageId,
            @RequestParam("user_id") UUID userId) {
        logger.info("DELETE /api/v1/conversations/{}/messages/{}", conversationId, messageId);
        messagingService.deleteMessage(conversationId, messageId, userId);
        return ResponseEntity.ok(Map.of("message", "Message deleted successfully"));
    }

    @PostMapping("/conversations/{id}/messages/{messageId}/reactions")
    @Operation(summary = "Add reaction", description = "Add a reaction to a message")
    public ResponseEntity<Map<String, String>> addReaction(
            @PathVariable("id") UUID conversationId,
            @PathVariable("messageId") UUID messageId,
            @RequestParam("user_id") UUID userId,
            @RequestParam("emoji") String emoji) {
        logger.info("POST /api/v1/conversations/{}/messages/{}/reactions - emoji: {}", conversationId, messageId, emoji);
        messagingService.addReaction(conversationId, messageId, userId, emoji);
        return ResponseEntity.ok(Map.of("message", "Reaction added successfully"));
    }

    @DeleteMapping("/conversations/{id}/messages/{messageId}/reactions/{emoji}")
    @Operation(summary = "Remove reaction", description = "Remove a reaction from a message")
    public ResponseEntity<Map<String, String>> removeReaction(
            @PathVariable("id") UUID conversationId,
            @PathVariable("messageId") UUID messageId,
            @PathVariable("emoji") String emoji,
            @RequestParam("user_id") UUID userId) {
        logger.info("DELETE /api/v1/conversations/{}/messages/{}/reactions/{}", conversationId, messageId, emoji);
        messagingService.removeReaction(conversationId, messageId, userId, emoji);
        return ResponseEntity.ok(Map.of("message", "Reaction removed successfully"));
    }

    @GetMapping("/conversations/{id}/messages/search")
    @Operation(summary = "Search messages", description = "Search messages in a conversation")
    public ResponseEntity<List<MessageDto>> searchMessages(
            @PathVariable("id") UUID conversationId,
            @RequestParam("user_id") UUID userId,
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "50") @Max(100) int limit) {
        logger.info("GET /api/v1/conversations/{}/messages/search - q={}", conversationId, query);
        return ResponseEntity.ok(messagingService.searchMessages(conversationId, userId, query, page, limit));
    }

    @GetMapping("/users/search")
    @Operation(summary = "Search users", description = "Search users for starting conversations")
    public ResponseEntity<List<ChatUserDto>> searchUsers(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") @Max(100) int limit,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        // Extract viewer_id from JWT for follow status
        UUID viewerId = null;
        try {
            viewerId = SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            logger.debug("Could not extract viewer_id from JWT: {}", e.getMessage());
        }
        logger.info("GET /api/v1/users/search - q={}, viewerId={}", query, viewerId);
        return ResponseEntity.ok(messagingService.searchUsers(query, page, limit, viewerId, authorization));
    }
    
    @PatchMapping("/messages/{messageId}/delivered")
    @Operation(summary = "Mark message delivered", description = "Recipient marks an inbound message as delivered (SENT → DELIVERED)")
    public ResponseEntity<Void> markMessageDelivered(@PathVariable UUID messageId) {
        UUID uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            throw new IllegalArgumentException("User not authenticated");
        }
        messagingService.markMessageDelivered(messageId, uid);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/messages/{messageId}/read")
    @Operation(summary = "Mark message read", description = "Recipient marks an inbound message as read")
    public ResponseEntity<Void> markMessageRead(@PathVariable UUID messageId) {
        UUID uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            throw new IllegalArgumentException("User not authenticated");
        }
        messagingService.markMessageRead(messageId, uid);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get total unread message count", description = "Get total number of unread messages across all conversations for the current user")
    public ResponseEntity<Map<String, Long>> getTotalUnreadCount() {
        try {
            UUID userId = SecurityUtils.getCurrentUserId();
            if (userId == null) {
                logger.warn("User not authenticated - cannot get unread count");
                return ResponseEntity.ok(Map.of("unread_count", 0L));
            }
            
            Long unreadCount = messagingService.getTotalUnreadCount(userId);
            logger.debug("GET /api/v1/unread-count - userId={}, unreadCount={}", userId, unreadCount);
            return ResponseEntity.ok(Map.of("unread_count", unreadCount));
        } catch (Exception e) {
            logger.error("Error getting total unread count: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("unread_count", 0L));
        }
    }
}

