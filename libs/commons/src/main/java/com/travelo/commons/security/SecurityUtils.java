package com.travelo.commons.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Utility class for extracting security context information.
 * Provides consistent way to get current user ID from JWT tokens.
 */
public class SecurityUtils {

    private SecurityUtils() {
        // Utility class
    }

    /**
     * Extracts the current user ID from the SecurityContext.
     * Returns null if not authenticated or user ID cannot be extracted.
     * 
     * @return User ID as UUID, or null if not available
     */
    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        // Handle JWT principal (OAuth2 Resource Server)
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim != null) {
                try {
                    if (userIdClaim instanceof String) {
                        return UUID.fromString((String) userIdClaim);
                    }
                    // Handle case where userId might be stored differently
                    return UUID.fromString(userIdClaim.toString());
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
            // Fallback: try to parse subject as UUID if it's not email
            // Note: This is a workaround for services that might use subject as userId
            // Ideally, all services should use userId claim
            try {
                String subject = jwt.getSubject();
                if (subject != null && subject.contains("-")) { // UUIDs contain dashes
                    return UUID.fromString(subject);
                }
            } catch (Exception e) {
                // Not a UUID, ignore
            }
        }

        // Fallback: try to get from principal name if it's a UUID
        try {
            String principalName = authentication.getName();
            if (principalName != null && !principalName.isEmpty()) {
                // If principal is email, we can't extract userId from it
                // This means JWT structure might be different
                return null;
            }
        } catch (Exception e) {
            // Ignore
        }

        return null;
    }

    /**
     * Extracts the current user ID as String from the SecurityContext.
     * 
     * @return User ID as String, or null if not available
     */
    public static String getCurrentUserIdAsString() {
        UUID userId = getCurrentUserId();
        return userId != null ? userId.toString() : null;
    }

    /**
     * Verifies that the current authenticated user matches the provided user ID.
     * Throws SecurityException if they don't match.
     * 
     * @param requestedUserId The user ID to verify against
     * @throws SecurityException if user IDs don't match
     */
    public static void verifyUserAccess(UUID requestedUserId) {
        UUID currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            throw new SecurityException("User not authenticated");
        }
        if (!currentUserId.equals(requestedUserId)) {
            throw new SecurityException("Access denied: User ID mismatch");
        }
    }

    /**
     * Verifies that the current authenticated user matches the provided user ID string.
     * 
     * @param requestedUserId The user ID string to verify against
     * @throws SecurityException if user IDs don't match or invalid format
     */
    public static void verifyUserAccess(String requestedUserId) {
        if (requestedUserId == null || requestedUserId.isEmpty()) {
            throw new SecurityException("User ID cannot be null or empty");
        }
        try {
            verifyUserAccess(UUID.fromString(requestedUserId));
        } catch (IllegalArgumentException e) {
            throw new SecurityException("Invalid user ID format: " + requestedUserId);
        }
    }

    /**
     * Checks if current user is authenticated.
     * 
     * @return true if authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}

