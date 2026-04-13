package com.travelo.authservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@Configuration
public class RateLimitingConfig implements WebMvcConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingConfig.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public RateLimitingConfig(@Qualifier("redisTemplate") @org.springframework.beans.factory.annotation.Autowired(required = false) RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Check if Redis is available
     */
    private boolean isRedisAvailable() {
        return redisTemplate != null;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginRateLimitInterceptor())
                .addPathPatterns("/api/v1/auth/login");
    }
    
    @Bean
    public HandlerInterceptor loginRateLimitInterceptor() {
        return new HandlerInterceptor() {
            private static final int MAX_REQUESTS_PER_IP = 10;
            private static final int WINDOW_SECONDS = 60; // 1 minute
            
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                if (!isRedisAvailable()) {
                    // Redis unavailable - allow request to proceed
                    return true;
                }
                try {
                    String clientIp = getClientIpAddress(request);
                    String key = "rate:login:ip:" + clientIp;
                    
                    String count = redisTemplate.opsForValue().get(key);
                    int requestCount = count == null ? 0 : Integer.parseInt(count);
                    
                    if (requestCount >= MAX_REQUESTS_PER_IP) {
                        response.setStatus(429); // HTTP 429 Too Many Requests
                        response.setContentType("application/json");
                        response.getWriter().write("{\"success\":false,\"message\":\"Too many login attempts. Please try again later.\",\"errorCode\":\"RATE_LIMIT_EXCEEDED\"}");
                        return false;
                    }
                    
                    requestCount++;
                    redisTemplate.opsForValue().set(key, String.valueOf(requestCount), WINDOW_SECONDS, TimeUnit.SECONDS);
                    
                    return true;
                } catch (org.springframework.dao.DataAccessException e) {
                    // Redis unavailable - allow request to proceed
                    logger.warn("Redis unavailable for rate limiting, allowing request: {}", e.getMessage());
                    return true;
                } catch (Exception e) {
                    // Any other error - allow request to proceed
                    logger.warn("Error in rate limiting, allowing request: {}", e.getMessage());
                    return true;
                }
            }
            
            private String getClientIpAddress(HttpServletRequest request) {
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("X-Real-IP");
                }
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                if (ip != null && ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        };
    }
}

