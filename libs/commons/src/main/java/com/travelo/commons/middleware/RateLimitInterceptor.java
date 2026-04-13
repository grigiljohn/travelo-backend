package com.travelo.commons.middleware;

import com.travelo.commons.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * Interceptor for rate limiting requests.
 * Uses Bucket4j token bucket algorithm.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private static final String RATE_LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";

    private final Map<String, Bucket> rateLimitBuckets;

    public RateLimitInterceptor(Map<String, Bucket> rateLimitBuckets) {
        this.rateLimitBuckets = rateLimitBuckets;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        // Skip health checks and actuator endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/actuator") || path.equals("/health")) {
            return true;
        }

        // Get user identifier (from JWT, API key, or IP)
        String userKey = getUserKey(request);
        
        // Get or create bucket for user using RateLimitConfig
        Bucket bucket = com.travelo.commons.config.RateLimitConfig.getDefaultBucket(rateLimitBuckets, userKey);

        // Try to consume a token
        if (!bucket.tryConsume(1)) {
            logger.warn("Rate limit exceeded for user: {}, path: {}", userKey, path);
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            try {
                response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
            } catch (java.io.IOException e) {
                logger.error("Error writing rate limit response", e);
            }
            return false;
        }

        // Add rate limit headers
        response.setHeader(RATE_LIMIT_HEADER, String.valueOf(RateLimitConfig.DEFAULT_RATE_LIMIT));
        response.setHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf(bucket.getAvailableTokens()));
        
        return true;
    }

    private String getUserKey(HttpServletRequest request) {
        // Try to get user ID from request attribute (set by auth filter)
        String userId = (String) request.getAttribute("user_id");
        if (userId != null) {
            return "user:" + userId;
        }

        // Try to get API key
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null) {
            return "api:" + apiKey;
        }

        // Fall back to IP address
        String ip = getClientIpAddress(request);
        return "ip:" + ip;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}

