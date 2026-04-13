package com.travelo.websocketservice.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelo.websocketservice.entity.CallStatus;
import com.travelo.websocketservice.entity.CallType;
import com.travelo.websocketservice.service.CallService;
import com.travelo.websocketservice.service.MessageBroadcastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket handler for real-time chat messaging.
 * Handles connection lifecycle and message routing.
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    
    /**
     * Multiple WebSocket connections per user (e.g. chat list + open thread, or several tabs).
     * All active sessions receive outbound events.
     */
    private final Map<UUID, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    // Store user ID by session ID
    private final Map<String, UUID> sessionToUser = new ConcurrentHashMap<>();
    
    private final MessageBroadcastService broadcastService;
    private final CallService callService;
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(
            MessageBroadcastService broadcastService,
            CallService callService,
            ObjectMapper objectMapper) {
        this.broadcastService = broadcastService;
        this.callService = callService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID userId = (UUID) session.getAttributes().get("userId");
        
        if (userId == null) {
            logger.warn("WebSocket session established without userId, closing connection");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        Set<WebSocketSession> sessions = userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>());
        boolean firstConnectionForUser = sessions.isEmpty();
        sessions.add(session);
        sessionToUser.put(session.getId(), userId);
        
        logger.info("WebSocket connection established for user: {} (session: {})", userId, session.getId());
        
        if (firstConnectionForUser) {
            broadcastService.notifyUserOnline(userId);
        }
        
        // Send connection confirmation
        sendMessage(session, createEventMessage("connected", Map.of("userId", userId.toString())));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        UUID userId = sessionToUser.get(session.getId());
        
        if (userId == null) {
            logger.warn("Received message from unknown session: {}", session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) payload.get("type");
            
            logger.debug("Received WebSocket message type: {} from user: {}", type, userId);
            
            switch (type) {
                case "message":
                    handleSendMessage(userId, payload);
                    break;
                case "typing":
                    handleTypingIndicator(userId, payload);
                    break;
                case "mark_read":
                    handleMarkAsRead(userId, payload);
                    break;
                case "reaction":
                    handleReaction(userId, payload);
                    break;
                case "call_initiate":
                    handleCallInitiate(userId, payload);
                    break;
                case "call_answer":
                    handleCallAnswer(userId, payload);
                    break;
                case "call_reject":
                    handleCallReject(userId, payload);
                    break;
                case "call_end":
                    handleCallEnd(userId, payload);
                    break;
                case "call_offer":
                    handleCallOffer(userId, payload);
                    break;
                case "call_answer_sdp":
                    handleCallAnswerSdp(userId, payload);
                    break;
                case "ice_candidate":
                    handleIceCandidate(userId, payload);
                    break;
                case "ping":
                    // Heartbeat
                    sendMessage(session, createEventMessage("pong", Map.of()));
                    break;
                default:
                    logger.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            logger.error("Error handling WebSocket message: {}", e.getMessage(), e);
            sendMessage(session, createEventMessage("error", Map.of("message", "Invalid message format")));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        UUID userId = sessionToUser.remove(session.getId());
        
        if (userId != null) {
            Set<WebSocketSession> set = userSessions.get(userId);
            boolean becameOffline = false;
            if (set != null) {
                set.remove(session);
                if (set.isEmpty()) {
                    userSessions.remove(userId);
                    becameOffline = true;
                }
            }
            logger.info("WebSocket connection closed for user: {} (status: {})", userId, status);
            
            if (becameOffline) {
                broadcastService.notifyUserOffline(userId);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        UUID userId = sessionToUser.get(session.getId());
        logger.error("WebSocket transport error for user: {} - {}", userId, exception.getMessage(), exception);
        
        if (userId != null) {
            Set<WebSocketSession> set = userSessions.get(userId);
            boolean becameOffline = false;
            if (set != null) {
                set.remove(session);
                if (set.isEmpty()) {
                    userSessions.remove(userId);
                    becameOffline = true;
                }
            }
            sessionToUser.remove(session.getId());
            if (becameOffline) {
                broadcastService.notifyUserOffline(userId);
            }
        }
    }

    /**
     * Send message to a specific user
     */
    public void sendToUser(UUID userId, Map<String, Object> message) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        for (WebSocketSession session : sessions) {
            if (session != null && session.isOpen()) {
                try {
                    sendMessage(session, message);
                } catch (Exception e) {
                    logger.error("Error sending message to user {}: {}", userId, e.getMessage());
                }
            }
        }
    }

    /**
     * Send message to multiple users
     */
    public void sendToUsers(Iterable<UUID> userIds, Map<String, Object> message) {
        for (UUID userId : userIds) {
            sendToUser(userId, message);
        }
    }

    /**
     * Check if user is online
     */
    public boolean isUserOnline(UUID userId) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null) {
            return false;
        }
        for (WebSocketSession session : sessions) {
            if (session != null && session.isOpen()) {
                return true;
            }
        }
        return false;
    }

    private void handleSendMessage(UUID senderId, Map<String, Object> payload) {
        // Forward to messaging-service via broadcast service
        broadcastService.handleSendMessage(senderId, payload);
    }

    private void handleTypingIndicator(UUID userId, Map<String, Object> payload) {
        String conversationId = (String) payload.get("conversation_id");
        Boolean isTyping = (Boolean) payload.get("is_typing");
        
        broadcastService.broadcastTypingIndicator(userId, conversationId, isTyping);
    }

    private void handleMarkAsRead(UUID userId, Map<String, Object> payload) {
        String conversationId = (String) payload.get("conversation_id");
        @SuppressWarnings("unchecked")
        java.util.List<String> messageIds = (java.util.List<String>) payload.get("message_ids");
        
        broadcastService.handleMarkAsRead(userId, conversationId, messageIds);
    }

    private void handleReaction(UUID userId, Map<String, Object> payload) {
        String messageId = (String) payload.get("message_id");
        String emoji = (String) payload.get("emoji");
        
        broadcastService.broadcastReaction(userId, messageId, emoji);
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> message) throws IOException {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        }
    }

    private Map<String, Object> createEventMessage(String type, Map<String, Object> data) {
        return Map.of(
            "type", type,
            "data", data,
            "timestamp", java.time.OffsetDateTime.now().toString()
        );
    }

    /**
     * Handle call initiation.
     */
    private void handleCallInitiate(UUID userId, Map<String, Object> payload) {
        try {
            String calleeIdStr = (String) payload.get("callee_id");
            String callTypeStr = (String) payload.getOrDefault("call_type", "VOICE");
            
            UUID calleeId = UUID.fromString(calleeIdStr);
            CallType callType = CallType.valueOf(callTypeStr.toUpperCase());
            
            // Check if callee is online
            if (!isUserOnline(calleeId)) {
                sendToUser(userId, createEventMessage("call_error", Map.of(
                    "message", "User is offline"
                )));
                return;
            }
            
            // Check if callee is already in a call
            if (callService.isUserInCall(calleeId)) {
                sendToUser(userId, createEventMessage("call_error", Map.of(
                    "message", "User is busy"
                )));
                return;
            }
            
            // Initiate call
            CallService.CallInfo callInfo = callService.initiateCall(userId, calleeId, callType);
            callService.updateCallStatus(callInfo.getCallId(), CallStatus.RINGING);
            
            // Notify caller
            sendToUser(userId, createEventMessage("call_initiated", Map.of(
                "call_id", callInfo.getCallId(),
                "callee_id", calleeId.toString(),
                "call_type", callType.name()
            )));
            
            // Notify callee
            sendToUser(calleeId, createEventMessage("call_incoming", Map.of(
                "call_id", callInfo.getCallId(),
                "caller_id", userId.toString(),
                "call_type", callType.name()
            )));
            
            logger.info("Call initiated: {} -> {} (type: {})", userId, calleeId, callType);
        } catch (Exception e) {
            logger.error("Error handling call initiate: {}", e.getMessage(), e);
            sendToUser(userId, createEventMessage("call_error", Map.of(
                "message", "Failed to initiate call"
            )));
        }
    }

    /**
     * Handle call answer.
     */
    private void handleCallAnswer(UUID userId, Map<String, Object> payload) {
        try {
            String callId = (String) payload.get("call_id");
            CallService.CallInfo callInfo = callService.getCall(callId);
            
            if (callInfo == null) {
                sendToUser(userId, createEventMessage("call_error", Map.of(
                    "message", "Call not found"
                )));
                return;
            }
            
            if (!callInfo.getCalleeId().equals(userId)) {
                sendToUser(userId, createEventMessage("call_error", Map.of(
                    "message", "Unauthorized"
                )));
                return;
            }
            
            callService.updateCallStatus(callId, CallStatus.CONNECTING);
            
            // Notify caller
            sendToUser(callInfo.getCallerId(), createEventMessage("call_answered", Map.of(
                "call_id", callId,
                "callee_id", userId.toString()
            )));
            
            // Notify callee
            sendToUser(userId, createEventMessage("call_connecting", Map.of(
                "call_id", callId
            )));
            
            logger.info("Call answered: {} (call: {})", userId, callId);
        } catch (Exception e) {
            logger.error("Error handling call answer: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle call reject.
     */
    private void handleCallReject(UUID userId, Map<String, Object> payload) {
        try {
            String callId = (String) payload.get("call_id");
            CallService.CallInfo callInfo = callService.getCall(callId);
            
            if (callInfo == null) return;
            
            callService.updateCallStatus(callId, CallStatus.REJECTED);
            
            // Notify caller
            sendToUser(callInfo.getCallerId(), createEventMessage("call_rejected", Map.of(
                "call_id", callId,
                "callee_id", userId.toString()
            )));
            
            logger.info("Call rejected: {} (call: {})", userId, callId);
        } catch (Exception e) {
            logger.error("Error handling call reject: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle call end.
     */
    private void handleCallEnd(UUID userId, Map<String, Object> payload) {
        try {
            String callId = (String) payload.get("call_id");
            CallService.CallInfo callInfo = callService.getCall(callId);
            
            if (callInfo == null) return;
            
            UUID otherUserId = callInfo.getCallerId().equals(userId) 
                ? callInfo.getCalleeId() 
                : callInfo.getCallerId();
            
            callService.endCall(callId, userId);
            
            // Notify other party
            sendToUser(otherUserId, createEventMessage("call_ended", Map.of(
                "call_id", callId,
                "ended_by", userId.toString()
            )));
            
            logger.info("Call ended: {} (call: {})", userId, callId);
        } catch (Exception e) {
            logger.error("Error handling call end: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle WebRTC offer (SDP).
     */
    private void handleCallOffer(UUID userId, Map<String, Object> payload) {
        try {
            String callId = (String) payload.get("call_id");
            String offer = (String) payload.get("offer");
            
            CallService.CallInfo callInfo = callService.getCall(callId);
            if (callInfo == null) return;
            
            UUID otherUserId = callInfo.getCallerId().equals(userId) 
                ? callInfo.getCalleeId() 
                : callInfo.getCallerId();
            
            // Forward offer to other party
            sendToUser(otherUserId, createEventMessage("call_offer", Map.of(
                "call_id", callId,
                "offer", offer,
                "from_user_id", userId.toString()
            )));
            
            logger.debug("Call offer forwarded: {} -> {} (call: {})", userId, otherUserId, callId);
        } catch (Exception e) {
            logger.error("Error handling call offer: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle WebRTC answer (SDP).
     */
    private void handleCallAnswerSdp(UUID userId, Map<String, Object> payload) {
        try {
            String callId = (String) payload.get("call_id");
            String answer = (String) payload.get("answer");
            
            CallService.CallInfo callInfo = callService.getCall(callId);
            if (callInfo == null) return;
            
            UUID otherUserId = callInfo.getCallerId().equals(userId) 
                ? callInfo.getCalleeId() 
                : callInfo.getCallerId();
            
            // Update call status to active
            callService.updateCallStatus(callId, CallStatus.ACTIVE);
            
            // Forward answer to other party
            sendToUser(otherUserId, createEventMessage("call_answer_sdp", Map.of(
                "call_id", callId,
                "answer", answer,
                "from_user_id", userId.toString()
            )));
            
            logger.debug("Call answer forwarded: {} -> {} (call: {})", userId, otherUserId, callId);
        } catch (Exception e) {
            logger.error("Error handling call answer SDP: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle ICE candidate exchange.
     */
    private void handleIceCandidate(UUID userId, Map<String, Object> payload) {
        try {
            String callId = (String) payload.get("call_id");
            @SuppressWarnings("unchecked")
            Map<String, Object> candidate = (Map<String, Object>) payload.get("candidate");
            
            CallService.CallInfo callInfo = callService.getCall(callId);
            if (callInfo == null) return;
            
            UUID otherUserId = callInfo.getCallerId().equals(userId) 
                ? callInfo.getCalleeId() 
                : callInfo.getCallerId();
            
            // Forward ICE candidate to other party
            sendToUser(otherUserId, createEventMessage("ice_candidate", Map.of(
                "call_id", callId,
                "candidate", candidate,
                "from_user_id", userId.toString()
            )));
            
            logger.debug("ICE candidate forwarded: {} -> {} (call: {})", userId, otherUserId, callId);
        } catch (Exception e) {
            logger.error("Error handling ICE candidate: {}", e.getMessage(), e);
        }
    }
}

