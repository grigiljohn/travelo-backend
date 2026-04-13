package com.travelo.mediaservice.entity;

/**
 * Media processing state lifecycle:
 * upload_pending -> processing -> ready | unsafe | review | infected
 */
public enum MediaStatus {
    UPLOAD_PENDING,  // Initial state after upload URL generation
    PROCESSING,      // Async processing in progress
    READY,           // Successfully processed and available
    UNSAFE,          // Content moderation marked as unsafe
    REVIEW,          // Requires human moderation review
    INFECTED         // Virus scan detected malware
}

