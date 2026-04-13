package com.travelo.websocketservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelo.messagingservice.dto.MessageDto;
import com.travelo.websocketservice.handler.ChatWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service to handle message broadcasting and integration with messaging-service.
 * Listens to Kafka events from messaging-service and broadcasts via WebSocket.
 */
@Service
public class MessageBroadcastService {

    private static final Logger logger = LoggerFactory.getLogger(MessageBroadcastService.class);
    
    private final ChatWebSocketHandler webSocketHandler;
    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /** In-process messaging REST base (same JVM after merge). */
    @Value("${app.messaging.internal-api-base:http://127.0.0.1:8098/api/v1}")
    private String messagingInternalApiBase;

    /**
     * When false (default), new messages are pushed over WebSocket from {@link com.travelo.messagingservice.service.impl.MessagingServiceImpl}
     * immediately after persistence (works without Kafka). Set true in multi-node deployments where only Kafka consumers should fan-out WS.
     */
    @Value("${app.messaging.kafka-ws-bridge-enabled:false}")
    private boolean kafkaWsBridgeEnabled;

    public MessageBroadcastService(
            @Lazy ChatWebSocketHandler webSocketHandler,
            RestTemplate restTemplate,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.webSocketHandler = webSocketHandler;
        this.restTemplate = restTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Push a persisted message to all online participants except the sender (sender already has the HTTP response).
     */
    public void broadcastPersistedMessage(MessageDto dto) {
        if (dto == null || dto.getConversationId() == null) {
            return;
        }
        try {
            Map<String, Object> map = objectMapper.convertValue(dto, new TypeReference<Map<String, Object>>() {});
            broadcastMessageToConversation(dto.getConversationId().toString(), map, dto.getSenderId());
        } catch (Exception e) {
            logger.error("broadcastPersistedMessage failed: {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> participantIdsFromConversationMap(Map<String, Object> conversation) {
        if (conversation == null) {
            return List.of();
        }
        Object raw = conversation.get("participants");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Object o : list) {
            if (o == null) {
                continue;
            }
            if (o instanceof UUID u) {
                out.add(u.toString());
            } else if (o instanceof String s && !s.isBlank()) {
                out.add(s.trim());
            } else {
                out.add(o.toString());
            }
        }
        return out;
    }

    /**
     * Handle message send request from WebSocket client.
     * Forwards to messaging-service and broadcasts to recipients.
     */
    public void handleSendMessage(UUID senderId, Map<String, Object> payload) {
        try {
            String conversationId = (String) payload.get("conversation_id");
            String content = (String) payload.get("content");
            String messageType = (String) payload.getOrDefault("type", "TEXT");
            String attachmentUrl = (String) payload.get("attachment_url");
            String replyToMessageId = (String) payload.get("reply_to_message_id");

            // Create message request matching CreateMessageRequest DTO (snake_case JSON)
            Map<String, Object> messageRequest = new HashMap<>();
            messageRequest.put("sender_id", senderId.toString());
            messageRequest.put("content", content);
            messageRequest.put("message_type", messageType);
            if (attachmentUrl != null) {
                messageRequest.put("attachment_url", attachmentUrl);
            }
            if (replyToMessageId != null) {
                messageRequest.put("reply_to_id", replyToMessageId);
            }

            // Call messaging-service to create message
            // Note: In production, use Feign client or service discovery
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restTemplate.postForObject(
                    messagingInternalApiBase + "/conversations/" + conversationId + "/messages",
                    messageRequest,
                    Map.class
                );

                if (response != null) {
                    broadcastMessageToConversation(conversationId, response, senderId);
                }
            } catch (Exception e) {
                logger.error("Error calling messaging-service: {}", e.getMessage());
                // Still try to broadcast optimistically
                Map<String, Object> optimisticMessage = new HashMap<>(payload);
                optimisticMessage.put("sender_id", senderId.toString());
                optimisticMessage.put("status", "SENT");
                broadcastMessageToConversation(conversationId, optimisticMessage, senderId);
            }
        } catch (Exception e) {
            logger.error("Error handling send message: {}", e.getMessage(), e);
        }
    }

    /**
     * Listen to Kafka events from messaging-service for new messages.
     * Broadcasts messages to all conversation participants via WebSocket.
     */
    @KafkaListener(topics = "dm.received", groupId = "realtime-service-ws")
    public void handleNewMessageEvent(Map<String, Object> event) {
        if (!kafkaWsBridgeEnabled) {
            return;
        }
        try {
            String conversationId = (String) event.get("conversationId");
            String messageId = (String) event.get("messageId");
            String recipientId = (String) event.get("recipientId");
            
            logger.debug("Received dm.received event: conversationId={}, messageId={}, recipientId={}", 
                    conversationId, messageId, recipientId);
            
            // Fetch message details from messaging-service
            Map<String, Object> message = null;
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> fetchedMessage = restTemplate.getForObject(
                    messagingInternalApiBase + "/conversations/" + conversationId + "/messages/" + messageId,
                    Map.class
                );
                message = fetchedMessage;
            } catch (Exception e) {
                logger.error("Error fetching message details from messaging-service: {}", e.getMessage(), e);
                // Try to construct minimal message from event data
                message = constructMessageFromEvent(event);
            }

            if (message != null) {
                // Broadcast to the recipient (and potentially all participants if needed)
                UUID recipient = UUID.fromString(recipientId);
                
                // Check if recipient is online before sending
                if (webSocketHandler.isUserOnline(recipient)) {
                    Map<String, Object> wsMessage = Map.of(
                        "type", "message",
                        "data", message,
                        "timestamp", java.time.OffsetDateTime.now().toString()
                    );
                    
                    logger.info("Broadcasting message {} to recipient {} via WebSocket", messageId, recipientId);
                    webSocketHandler.sendToUser(recipient, wsMessage);
                } else {
                    logger.debug("Recipient {} is offline, message {} will be delivered when they connect", 
                            recipientId, messageId);
                }
                
                // Also broadcast to all other online participants in the conversation
                // This ensures all participants see the message in real-time
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> conversation = restTemplate.getForObject(
                        messagingInternalApiBase + "/conversations/" + conversationId + "?user_id=" + recipientId,
                        Map.class
                    );

                    if (conversation != null) {
                        List<String> participantIds = participantIdsFromConversationMap(conversation);
                        if (!participantIds.isEmpty()) {
                            String senderId = (String) event.get("senderId");
                            
                            Map<String, Object> wsMessage = Map.of(
                                "type", "message",
                                "data", message,
                                "timestamp", java.time.OffsetDateTime.now().toString()
                            );
                            
                            // Send to all participants except sender (they already have it)
                            for (String participantIdStr : participantIds) {
                                try {
                                    UUID participantId = UUID.fromString(participantIdStr);
                                    
                                    // Skip sender (they already sent it via their own client)
                                    if (senderId != null && participantIdStr.equals(senderId)) {
                                        continue;
                                    }
                                    
                                    // Skip the main recipient (already handled above)
                                    if (participantIdStr.equals(recipientId)) {
                                        continue;
                                    }
                                    
                                    // Send to other online participants
                                    if (webSocketHandler.isUserOnline(participantId)) {
                                        logger.debug("Broadcasting message {} to participant {} via WebSocket", 
                                                messageId, participantIdStr);
                                        webSocketHandler.sendToUser(participantId, wsMessage);
                                    }
                                } catch (IllegalArgumentException e) {
                                    logger.warn("Invalid participant ID in event: {}", participantIdStr);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Error broadcasting to all participants (non-critical): {}", e.getMessage());
                    // Non-critical: main recipient already handled above
                }
            } else {
                logger.error("Could not construct message for WebSocket broadcast");
            }
        } catch (Exception e) {
            logger.error("Error handling new message event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Construct minimal message object from Kafka event data when API call fails.
     */
    private Map<String, Object> constructMessageFromEvent(Map<String, Object> event) {
        Map<String, Object> message = new HashMap<>();
        message.put("id", event.get("messageId"));
        message.put("conversation_id", event.get("conversationId"));
        message.put("sender_id", event.get("senderId"));
        message.put("content", event.get("messagePreview"));
        message.put("message_type", "TEXT");
        message.put("status", "SENT");
        message.put("created_at", java.time.OffsetDateTime.now().toString());
        return message;
    }

    /**
     * Broadcast message to all online participants. Skips {@code skipDeliveryToUserId} (typically the sender).
     */
    private void broadcastMessageToConversation(String conversationId, Map<String, Object> message, UUID skipDeliveryToUserId) {
        try {
            Object sidObj = message.get("sender_id");
            String userIdQuery = sidObj != null ? sidObj.toString() : null;
            if (userIdQuery == null || userIdQuery.isBlank()) {
                logger.warn("broadcastMessageToConversation: missing sender_id, cannot load participants");
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> conversation = restTemplate.getForObject(
                messagingInternalApiBase + "/conversations/" + conversationId + "?user_id=" + userIdQuery,
                Map.class
            );

            if (conversation != null) {
                List<String> participantIds = participantIdsFromConversationMap(conversation);
                if (!participantIds.isEmpty()) {
                    Map<String, Object> wsMessage = Map.of(
                        "type", "message",
                        "data", message,
                        "timestamp", java.time.OffsetDateTime.now().toString()
                    );

                    for (String participantIdStr : participantIds) {
                        UUID participantId = UUID.fromString(participantIdStr);
                        if (skipDeliveryToUserId != null && skipDeliveryToUserId.equals(participantId)) {
                            continue;
                        }
                        if (webSocketHandler.isUserOnline(participantId)) {
                            webSocketHandler.sendToUser(participantId, wsMessage);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error broadcasting message to conversation: {}", e.getMessage());
        }
    }

    /**
     * Broadcast typing indicator to conversation participants.
     */
    public void broadcastTypingIndicator(UUID userId, String conversationId, Boolean isTyping) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> conversation = restTemplate.getForObject(
                messagingInternalApiBase + "/conversations/" + conversationId + "?user_id=" + userId,
                Map.class
            );

            if (conversation != null) {
                List<String> participantIds = participantIdsFromConversationMap(conversation);
                if (!participantIds.isEmpty()) {
                    Map<String, Object> typingEvent = Map.of(
                        "type", "typing",
                        "data", Map.of(
                            "user_id", userId.toString(),
                            "conversation_id", conversationId,
                            "is_typing", isTyping,
                            "timestamp", java.time.OffsetDateTime.now().toString()
                        )
                    );

                    for (String participantIdStr : participantIds) {
                        UUID participantId = UUID.fromString(participantIdStr);
                        if (!participantId.equals(userId) && webSocketHandler.isUserOnline(participantId)) {
                            webSocketHandler.sendToUser(participantId, typingEvent);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error broadcasting typing indicator: {}", e.getMessage());
        }
    }

    /**
     * Handle mark as read request.
     */
    public void handleMarkAsRead(UUID userId, String conversationId, List<String> messageIds) {
        try {
            // Call messaging-service to mark as read
            restTemplate.postForObject(
                messagingInternalApiBase + "/conversations/" + conversationId + "/read?user_id=" + userId,
                null,
                Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> conversation = restTemplate.getForObject(
                messagingInternalApiBase + "/conversations/" + conversationId + "?user_id=" + userId,
                Map.class
            );

            if (conversation != null) {
                @SuppressWarnings("unchecked")
                List<String> participantIds = (List<String>) conversation.get("participants");
                
                if (participantIds != null) {
                    Map<String, Object> readEvent = Map.of(
                        "type", "message_read",
                        "data", Map.of(
                            "user_id", userId.toString(),
                            "conversation_id", conversationId,
                            "message_ids", messageIds != null ? messageIds : List.of(),
                            "timestamp", java.time.OffsetDateTime.now().toString()
                        )
                    );

                    for (String participantIdStr : participantIds) {
                        UUID participantId = UUID.fromString(participantIdStr);
                        if (!participantId.equals(userId) && webSocketHandler.isUserOnline(participantId)) {
                            webSocketHandler.sendToUser(participantId, readEvent);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error handling mark as read: {}", e.getMessage());
        }
    }

    /**
     * Broadcast reaction to message.
     */
    public void broadcastReaction(UUID userId, String messageId, String emoji) {
        try {
            Map<String, Object> reactionEvent = Map.of(
                "type", "reaction",
                "data", Map.of(
                    "user_id", userId.toString(),
                    "message_id", messageId,
                    "emoji", emoji,
                    "timestamp", java.time.OffsetDateTime.now().toString()
                )
            );

            // TODO: Fetch conversation ID from message and broadcast to participants
            // For now, this is a placeholder
            logger.info("Reaction broadcast: user={}, message={}, emoji={}", userId, messageId, emoji);
        } catch (Exception e) {
            logger.error("Error broadcasting reaction: {}", e.getMessage());
        }
    }

    /**
     * Notify that user is online.
     * Broadcasts presence update to all participants in user's active conversations.
     */
    public void notifyUserOnline(UUID userId) {
        logger.info("User online: {}", userId);
        try {
            // Fetch user's active conversations from messaging-service
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> conversations = restTemplate.getForObject(
                messagingInternalApiBase + "/conversations?user_id=" + userId + "&limit=100",
                List.class
            );

            if (conversations != null) {
                Map<String, Object> presenceEvent = Map.of(
                    "type", "presence",
                    "data", Map.of(
                        "user_id", userId.toString(),
                        "is_online", true,
                        "timestamp", java.time.OffsetDateTime.now().toString()
                    ),
                    "timestamp", java.time.OffsetDateTime.now().toString()
                );

                // Broadcast to all participants in user's conversations
                for (Map<String, Object> conversation : conversations) {
                    List<String> participantIds = participantIdsFromConversationMap(conversation);
                    for (String participantIdStr : participantIds) {
                        try {
                            UUID participantId = UUID.fromString(participantIdStr);
                            if (!participantId.equals(userId) && webSocketHandler.isUserOnline(participantId)) {
                                webSocketHandler.sendToUser(participantId, presenceEvent);
                            }
                        } catch (IllegalArgumentException e) {
                            logger.warn("Invalid participant ID: {}", participantIdStr);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error notifying user online: {}", e.getMessage(), e);
        }
    }

    /**
     * Notify that user is offline.
     * Broadcasts presence update to all participants in user's active conversations.
     */
    public void notifyUserOffline(UUID userId) {
        logger.info("User offline: {}", userId);
        try {
            // Fetch user's active conversations from messaging-service
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> conversations = restTemplate.getForObject(
                messagingInternalApiBase + "/conversations?user_id=" + userId + "&limit=100",
                List.class
            );

            if (conversations != null) {
                Map<String, Object> presenceEvent = Map.of(
                    "type", "presence",
                    "data", Map.of(
                        "user_id", userId.toString(),
                        "is_online", false,
                        "last_seen", java.time.OffsetDateTime.now().toString(),
                        "timestamp", java.time.OffsetDateTime.now().toString()
                    ),
                    "timestamp", java.time.OffsetDateTime.now().toString()
                );

                // Broadcast to all participants in user's conversations
                for (Map<String, Object> conversation : conversations) {
                    List<String> participantIds = participantIdsFromConversationMap(conversation);
                    for (String participantIdStr : participantIds) {
                        try {
                            UUID participantId = UUID.fromString(participantIdStr);
                            if (!participantId.equals(userId) && webSocketHandler.isUserOnline(participantId)) {
                                webSocketHandler.sendToUser(participantId, presenceEvent);
                            }
                        } catch (IllegalArgumentException e) {
                            logger.warn("Invalid participant ID: {}", participantIdStr);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error notifying user offline: {}", e.getMessage(), e);
        }
    }
}

