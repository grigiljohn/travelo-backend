package com.travelo.authservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    private final com.travelo.authservice.service.JwtService jwtService;
    
    @Value("${app.jwt.access-token-expiration:3600}")
    private Integer accessTokenExpiration;
    
    public TokenBlacklistService(@Qualifier("redisTemplate") @org.springframework.beans.factory.annotation.Autowired(required = false) RedisTemplate<String, String> redisTemplate,
                                 com.travelo.authservice.service.JwtService jwtService) {
        this.redisTemplate = redisTemplate;
        this.jwtService = jwtService;
    }
    
    /**
     * Check if Redis is available
     */
    private boolean isRedisAvailable() {
        return redisTemplate != null;
    }
    
    /**
     * Add token to blacklist
     * Gracefully degrades if Redis is unavailable (token blacklisting won't work)
     */
    public void blacklistToken(String token) {
        if (!isRedisAvailable()) {
            logger.debug("Redis unavailable, skipping token blacklisting");
            return;
        }
        try {
            // Calculate remaining TTL from token expiration
            if (jwtService.isTokenExpired(token)) {
                // Token already expired, no need to blacklist
                return;
            }
            
            java.util.Date expiration = jwtService.extractExpiration(token);
            long ttl = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            
            if (ttl > 0) {
                String key = "blacklist:token:" + token;
                redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.SECONDS);
                logger.debug("Token blacklisted: {}", token.substring(0, Math.min(20, token.length())));
            }
        } catch (org.springframework.dao.DataAccessException e) {
            logger.warn("Redis unavailable for token blacklisting (non-critical): {}", e.getMessage());
            // Token blacklisting won't work, but authentication will still work
        } catch (Exception e) {
            logger.warn("Error blacklisting token (non-critical): {}", e.getMessage());
        }
    }
    
    /**
     * Check if token is blacklisted
     * Returns false if Redis is unavailable (allows token to be used)
     */
    public boolean isTokenBlacklisted(String token) {
        if (!isRedisAvailable()) {
            return false; // Allow token if Redis is unavailable
        }
        try {
            String key = "blacklist:token:" + token;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (org.springframework.dao.DataAccessException e) {
            logger.warn("Redis unavailable for token blacklist check, allowing token: {}", e.getMessage());
            return false; // Allow token if Redis is unavailable
        } catch (Exception e) {
            logger.warn("Error checking token blacklist, allowing token: {}", e.getMessage());
            return false; // Allow token on error
        }
    }
    
    /**
     * Blacklist all tokens for a user (on password change, account deletion, etc.)
     */
    public void blacklistAllUserTokens(String email) {
        // In a more sophisticated system, you might track all issued tokens
        // For now, we'll rely on token expiration and refresh token rotation
        logger.info("Blacklisting all tokens for user: {}", email);
        // This could be enhanced to track active sessions
    }
}

