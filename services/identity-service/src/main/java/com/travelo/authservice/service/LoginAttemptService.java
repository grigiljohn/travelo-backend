package com.travelo.authservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {
    
    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${app.login.max-attempts:5}")
    private Integer maxAttempts;
    
    @Value("${app.login.lockout-duration:900}") // 15 minutes default
    private Integer lockoutDurationSeconds;
    
    @Value("${app.login.attempt-window:900}") // 15 minutes default
    private Integer attemptWindowSeconds;
    
    public LoginAttemptService(@Qualifier("redisTemplate") @org.springframework.beans.factory.annotation.Autowired(required = false) RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Check if Redis is available
     */
    private boolean isRedisAvailable() {
        return redisTemplate != null;
    }
    
    /**
     * Safely execute Redis operation with fallback
     */
    private <T> T executeRedisOperation(java.util.function.Supplier<T> operation, T fallbackValue) {
        try {
            return operation.get();
        } catch (DataAccessException e) {
            logger.warn("Redis operation failed, using fallback: {}", e.getMessage());
            return fallbackValue;
        } catch (Exception e) {
            logger.warn("Unexpected error during Redis operation, using fallback: {}", e.getMessage());
            return fallbackValue;
        }
    }
    
    /**
     * Safely execute Redis operation without return value
     */
    private void executeRedisOperation(Runnable operation) {
        try {
            operation.run();
        } catch (DataAccessException e) {
            logger.warn("Redis operation failed (non-critical): {}", e.getMessage());
        } catch (Exception e) {
            logger.warn("Unexpected error during Redis operation (non-critical): {}", e.getMessage());
        }
    }
    
    /**
     * Record a failed login attempt
     * Gracefully degrades if Redis is unavailable
     */
    public void recordFailedAttempt(String emailOrUsername) {
        if (!isRedisAvailable()) {
            logger.debug("Redis unavailable, skipping failed attempt tracking");
            return;
        }
        executeRedisOperation(() -> {
            String key = "login:attempts:" + emailOrUsername;
            String attempts = redisTemplate.opsForValue().get(key);
            
            int attemptCount = attempts == null ? 0 : Integer.parseInt(attempts);
            attemptCount++;
            
            redisTemplate.opsForValue().set(key, String.valueOf(attemptCount), attemptWindowSeconds, TimeUnit.SECONDS);
            
            // If max attempts reached, lock the account
            if (attemptCount >= maxAttempts) {
                lockAccount(emailOrUsername);
                logger.warn("Account locked due to too many failed login attempts: {}", emailOrUsername);
            }
        });
    }
    
    /**
     * Clear failed attempts on successful login
     * Gracefully degrades if Redis is unavailable
     */
    public void clearFailedAttempts(String emailOrUsername) {
        if (!isRedisAvailable()) {
            return;
        }
        executeRedisOperation(() -> {
            String key = "login:attempts:" + emailOrUsername;
            redisTemplate.delete(key);
            
            // Also unlock if locked
            String lockKey = "login:locked:" + emailOrUsername;
            redisTemplate.delete(lockKey);
        });
    }
    
    /**
     * Check if account is locked
     * Returns false if Redis is unavailable (allows login to proceed)
     */
    public boolean isAccountLocked(String emailOrUsername) {
        return executeRedisOperation(() -> {
            String lockKey = "login:locked:" + emailOrUsername;
            return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
        }, false); // Fallback: allow login if Redis is unavailable
    }
    
    /**
     * Get remaining lockout time in seconds
     * Returns 0 if Redis is unavailable
     */
    public Long getRemainingLockoutTime(String emailOrUsername) {
        return executeRedisOperation(() -> {
            String lockKey = "login:locked:" + emailOrUsername;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
                return redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            }
            return 0L;
        }, 0L); // Fallback: no lockout if Redis is unavailable
    }
    
    /**
     * Get remaining attempts before lockout
     * Returns max attempts if Redis is unavailable
     */
    public Integer getRemainingAttempts(String emailOrUsername) {
        return executeRedisOperation(() -> {
            String key = "login:attempts:" + emailOrUsername;
            String attempts = redisTemplate.opsForValue().get(key);
            
            if (attempts == null) {
                return maxAttempts;
            }
            
            int attemptCount = Integer.parseInt(attempts);
            return Math.max(0, maxAttempts - attemptCount);
        }, maxAttempts); // Fallback: allow all attempts if Redis is unavailable
    }
    
    /**
     * Lock the account
     * Gracefully degrades if Redis is unavailable
     */
    private void lockAccount(String emailOrUsername) {
        if (!isRedisAvailable()) {
            return;
        }
        executeRedisOperation(() -> {
            String lockKey = "login:locked:" + emailOrUsername;
            redisTemplate.opsForValue().set(lockKey, "1", lockoutDurationSeconds, TimeUnit.SECONDS);
        });
    }
}

