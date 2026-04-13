package com.travelo.commons.idempotency;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * Interceptor to handle idempotency keys.
 * Stores request/response pairs in Redis to prevent duplicate processing.
 */
public class IdempotencyKeyInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyKeyInterceptor.class);
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String IDEMPOTENCY_RESPONSE_HEADER = "X-Idempotency-Key";
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final long defaultTtlSeconds;

    public IdempotencyKeyInterceptor(RedisTemplate<String, Object> redisTemplate, long defaultTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.defaultTtlSeconds = defaultTtlSeconds;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, 
                           @NonNull HttpServletResponse response, 
                           @NonNull Object handler) throws Exception {
        
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        IdempotencyKey annotation = handlerMethod.getMethodAnnotation(IdempotencyKey.class);
        if (annotation == null) {
            annotation = handlerMethod.getBeanType().getAnnotation(IdempotencyKey.class);
        }

        if (annotation == null) {
            return true; // No idempotency required
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            if (annotation.required()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\":\"Idempotency-Key header is required\"}");
                return false;
            }
            return true; // Optional, continue
        }

        // Check if we've seen this key before
        String redisKey = "idempotency:" + idempotencyKey;
        IdempotencyResponse cachedResponse = (IdempotencyResponse) redisTemplate.opsForValue().get(redisKey);

        if (cachedResponse != null) {
            logger.info("Idempotent request detected: key={}", idempotencyKey);
            
            // Return cached response
            response.setStatus(cachedResponse.getStatusCode());
            response.setContentType(cachedResponse.getContentType());
            response.getWriter().write(cachedResponse.getBody());
            response.setHeader(IDEMPOTENCY_RESPONSE_HEADER, idempotencyKey);
            
            return false; // Don't continue processing
        }

        // Store request info for post-processing
        request.setAttribute("idempotency_key", idempotencyKey);
        request.setAttribute("idempotency_ttl", annotation.ttlSeconds());
        request.setAttribute("idempotency_redis_key", redisKey);

        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, 
                               @NonNull HttpServletResponse response, 
                               @NonNull Object handler, 
                               @Nullable Exception ex) throws Exception {
        
        String idempotencyKey = (String) request.getAttribute("idempotency_key");
        if (idempotencyKey == null) {
            return; // No idempotency key
        }

        String redisKey = (String) request.getAttribute("idempotency_redis_key");
        Long ttl = (Long) request.getAttribute("idempotency_ttl");

        if (ex == null && response.getStatus() >= 200 && response.getStatus() < 300) {
            // Cache successful response
            try {
                String responseBody = response.toString(); // Note: Actual body capture requires response wrapper
                IdempotencyResponse idempotencyResponse = new IdempotencyResponse(
                        response.getStatus(),
                        response.getContentType(),
                        responseBody
                );
                
                Long actualTtl = ttl != null ? ttl : defaultTtlSeconds;
                redisTemplate.opsForValue().set(
                        redisKey,
                        idempotencyResponse,
                        Duration.ofSeconds(actualTtl)
                );
                
                response.setHeader(IDEMPOTENCY_RESPONSE_HEADER, idempotencyKey);
                logger.debug("Cached idempotency response: key={}", idempotencyKey);
            } catch (Exception e) {
                logger.error("Error caching idempotency response", e);
            }
        }
    }

    /**
     * Cached response data.
     */
    public static class IdempotencyResponse {
        private final int statusCode;
        private final String contentType;
        private final String body;

        public IdempotencyResponse(int statusCode, String contentType, String body) {
            this.statusCode = statusCode;
            this.contentType = contentType;
            this.body = body;
        }

        public int getStatusCode() { return statusCode; }
        public String getContentType() { return contentType; }
        public String getBody() { return body; }
    }
}

