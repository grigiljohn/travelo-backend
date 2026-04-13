package com.travelo.websocketservice.service;

import com.travelo.websocketservice.entity.CallStatus;
import com.travelo.websocketservice.entity.CallType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage call state and signaling.
 * Handles call lifecycle: initiate, accept, reject, end.
 */
@Service
public class CallService {

    private static final Logger logger = LoggerFactory.getLogger(CallService.class);

    // Store active calls: callId -> CallInfo
    private final Map<String, CallInfo> activeCalls = new ConcurrentHashMap<>();
    // Store user's current call: userId -> callId
    private final Map<UUID, String> userActiveCalls = new ConcurrentHashMap<>();

    /**
     * Initiate a new call.
     */
    public CallInfo initiateCall(UUID callerId, UUID calleeId, CallType callType) {
        String callId = UUID.randomUUID().toString();
        
        CallInfo callInfo = new CallInfo();
        callInfo.setCallId(callId);
        callInfo.setCallerId(callerId);
        callInfo.setCalleeId(calleeId);
        callInfo.setCallType(callType);
        callInfo.setStatus(CallStatus.INITIATING);
        callInfo.setStartedAt(OffsetDateTime.now());
        
        activeCalls.put(callId, callInfo);
        userActiveCalls.put(callerId, callId);
        
        logger.info("Call initiated: {} -> {} (type: {})", callerId, calleeId, callType);
        return callInfo;
    }

    /**
     * Update call status.
     */
    public void updateCallStatus(String callId, CallStatus status) {
        CallInfo callInfo = activeCalls.get(callId);
        if (callInfo != null) {
            callInfo.setStatus(status);
            if (status == CallStatus.ACTIVE) {
                callInfo.setConnectedAt(OffsetDateTime.now());
            } else if (status == CallStatus.ENDED || 
                      status == CallStatus.REJECTED || 
                      status == CallStatus.MISSED ||
                      status == CallStatus.CANCELLED) {
                callInfo.setEndedAt(OffsetDateTime.now());
                // Clean up
                userActiveCalls.remove(callInfo.getCallerId());
                userActiveCalls.remove(callInfo.getCalleeId());
            }
            logger.info("Call {} status updated to: {}", callId, status);
        }
    }

    /**
     * Get call information.
     */
    public CallInfo getCall(String callId) {
        return activeCalls.get(callId);
    }

    /**
     * Get user's active call.
     */
    public CallInfo getUserActiveCall(UUID userId) {
        String callId = userActiveCalls.get(userId);
        return callId != null ? activeCalls.get(callId) : null;
    }

    /**
     * End a call.
     */
    public void endCall(String callId, UUID endedBy) {
        CallInfo callInfo = activeCalls.get(callId);
        if (callInfo != null) {
            callInfo.setStatus(CallStatus.ENDED);
            callInfo.setEndedAt(OffsetDateTime.now());
            callInfo.setEndedBy(endedBy);
            
            userActiveCalls.remove(callInfo.getCallerId());
            userActiveCalls.remove(callInfo.getCalleeId());
            
            // Keep in map for history (can be cleaned up later)
            logger.info("Call {} ended by user {}", callId, endedBy);
        }
    }

    /**
     * Check if user is in a call.
     */
    public boolean isUserInCall(UUID userId) {
        return userActiveCalls.containsKey(userId);
    }

    /**
     * Call information holder.
     */
    public static class CallInfo {
        private String callId;
        private UUID callerId;
        private UUID calleeId;
        private CallType callType;
        private CallStatus status;
        private OffsetDateTime startedAt;
        private OffsetDateTime connectedAt;
        private OffsetDateTime endedAt;
        private UUID endedBy;
        private Long durationSeconds;

        // Getters and Setters
        public String getCallId() { return callId; }
        public void setCallId(String callId) { this.callId = callId; }
        public UUID getCallerId() { return callerId; }
        public void setCallerId(UUID callerId) { this.callerId = callerId; }
        public UUID getCalleeId() { return calleeId; }
        public void setCalleeId(UUID calleeId) { this.calleeId = calleeId; }
        public CallType getCallType() { return callType; }
        public void setCallType(CallType callType) { this.callType = callType; }
        public CallStatus getStatus() { return status; }
        public void setStatus(CallStatus status) { this.status = status; }
        public OffsetDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
        public OffsetDateTime getConnectedAt() { return connectedAt; }
        public void setConnectedAt(OffsetDateTime connectedAt) { this.connectedAt = connectedAt; }
        public OffsetDateTime getEndedAt() { return endedAt; }
        public void setEndedAt(OffsetDateTime endedAt) { this.endedAt = endedAt; }
        public UUID getEndedBy() { return endedBy; }
        public void setEndedBy(UUID endedBy) { this.endedBy = endedBy; }
        public Long getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
    }
}

