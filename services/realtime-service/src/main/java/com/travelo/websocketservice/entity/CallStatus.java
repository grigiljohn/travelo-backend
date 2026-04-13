package com.travelo.websocketservice.entity;

public enum CallStatus {
    INITIATING,    // Call is being initiated
    RINGING,       // Call is ringing (waiting for answer)
    CONNECTING,    // Call is connecting (answered, establishing connection)
    ACTIVE,        // Call is active
    ENDED,         // Call ended normally
    REJECTED,      // Call was rejected
    MISSED,        // Call was missed (not answered)
    CANCELLED      // Call was cancelled by caller
}

