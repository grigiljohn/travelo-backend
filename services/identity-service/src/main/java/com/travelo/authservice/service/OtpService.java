package com.travelo.authservice.service;

import com.travelo.authservice.entity.Otp;
import com.travelo.authservice.repository.OtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {
    
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    
    private final OtpRepository otpRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom random = new SecureRandom();
    
    @Value("${app.otp.expiration}")
    private Integer otpExpiration;
    
    @Value("${app.otp.length}")
    private Integer otpLength;
    
    @Value("${app.otp.resend-cooldown}")
    private Integer resendCooldown;
    
    @Value("${app.otp.rate-limit.max-requests}")
    private Integer maxRequests;
    
    @Value("${app.otp.rate-limit.window-minutes}")
    private Integer windowMinutes;
    
    @Value("${app.otp.verification-rate-limit.max-attempts}")
    private Integer maxVerificationAttempts;
    
    @Value("${app.otp.verification-rate-limit.window-seconds}")
    private Integer verificationWindowSeconds;
    
    @Value("${app.otp.default-otp:123456}")
    private String defaultOtp;
    
    @Value("${app.otp.use-default-otp:false}")
    private Boolean useDefaultOtp;
    
    public OtpService(OtpRepository otpRepository, 
                     @Qualifier("redisTemplate") @org.springframework.beans.factory.annotation.Autowired(required = false) RedisTemplate<String, String> redisTemplate) {
        this.otpRepository = otpRepository;
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Check if Redis is available
     */
    private boolean isRedisAvailable() {
        return redisTemplate != null;
    }
    
    @Transactional
    public String generateOtp(UUID userId, String email) {
        // Check rate limit
        if (!checkRateLimit(email)) {
            throw new RuntimeException("Too many OTP requests. Please try again later.");
        }
        
        // Check cooldown (gracefully handle Redis unavailability)
        if (isRedisAvailable()) {
            try {
                String cooldownKey = "otp:cooldown:" + email;
                if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
                    Long remaining = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
                    throw new RuntimeException("Please wait " + remaining + " seconds before requesting a new OTP.");
                }
            } catch (RuntimeException e) {
                if (e.getMessage().contains("Please wait")) {
                    throw e; // Re-throw cooldown exception
                }
                logger.warn("Redis unavailable for cooldown check, proceeding: {}", e.getMessage());
            } catch (Exception e) {
                logger.warn("Redis unavailable for cooldown check, proceeding: {}", e.getMessage());
            }
        }
        
        // Generate OTP
        String otp = useDefaultOtp ? defaultOtp : generateRandomOtp();
        
        if (useDefaultOtp) {
            logger.info("Using default OTP for development/testing: {}", defaultOtp);
        }
        
        // Store in Redis with expiration (gracefully handle Redis unavailability)
        if (isRedisAvailable()) {
            try {
                String redisKey = "otp:" + email;
                redisTemplate.opsForValue().set(redisKey, otp, otpExpiration, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warn("Redis unavailable for OTP storage, using database only: {}", e.getMessage());
            }
        }
        
        // Store in database (always works)
        OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(otpExpiration);
        Otp otpEntity = new Otp(userId, email, otp, expiresAt);
        otpRepository.save(otpEntity);
        
        // Set cooldown (gracefully handle Redis unavailability)
        if (isRedisAvailable()) {
            try {
                String cooldownKey = "otp:cooldown:" + email;
                redisTemplate.opsForValue().set(cooldownKey, "1", resendCooldown, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warn("Redis unavailable for cooldown setting (non-critical): {}", e.getMessage());
            }
        }
        
        // Update rate limit counter
        updateRateLimitCounter(email);
        
        logger.info("OTP generated for email: {}", email);
        return otp;
    }
    
    @Transactional
    public Boolean verifyOtp(String email, String otp) {
        // Check verification rate limit
        if (!checkVerificationRateLimit(email)) {
            throw new RuntimeException("Too many verification attempts. Please try again later.");
        }
        
        // Check Redis first (gracefully fallback to database if unavailable)
        String redisKey = "otp:" + email;
        String storedOtp = null;
        if (isRedisAvailable()) {
            try {
                storedOtp = redisTemplate.opsForValue().get(redisKey);
            } catch (Exception e) {
                logger.warn("Redis unavailable for OTP verification, using database: {}", e.getMessage());
            }
        }
        
        // Verify in database (primary source of truth)
        Otp otpEntity = otpRepository.findByEmailAndOtpAndIsUsedFalse(email, otp)
                .orElse(null);
        
        if (otpEntity == null) {
            updateVerificationRateLimitCounter(email);
            return false;
        }
        
        if (otpEntity.getExpiresAt().isBefore(OffsetDateTime.now())) {
            updateVerificationRateLimitCounter(email);
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }
        
        // If Redis had the OTP, verify it matches (optional check)
        if (storedOtp != null && !storedOtp.equals(otp)) {
            updateVerificationRateLimitCounter(email);
            return false;
        }
        
        // Mark as used
        otpEntity.setIsUsed(true);
        otpRepository.save(otpEntity);
        
        // Remove from Redis (gracefully handle failure)
        if (isRedisAvailable()) {
            try {
                redisTemplate.delete(redisKey);
            } catch (Exception e) {
                logger.warn("Redis unavailable for OTP deletion (non-critical): {}", e.getMessage());
            }
        }
        
        logger.info("OTP verified successfully for email: {}", email);
        return true;
    }
    
    private String generateRandomOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
    
    private Boolean checkRateLimit(String email) {
        if (!isRedisAvailable()) {
            logger.debug("Redis unavailable for rate limiting, allowing request");
            return true; // Allow request if Redis is unavailable
        }
        try {
            String key = "otp:rate:" + email;
            String count = redisTemplate.opsForValue().get(key);
            
            if (count == null) {
                return true;
            }
            
            int requestCount = Integer.parseInt(count);
            return requestCount < maxRequests;
        } catch (Exception e) {
            logger.warn("Redis unavailable for rate limiting, allowing request: {}", e.getMessage());
            return true; // Allow request if Redis is unavailable
        }
    }
    
    private void updateRateLimitCounter(String email) {
        if (!isRedisAvailable()) {
            return; // Skip if Redis is unavailable
        }
        try {
            String key = "otp:rate:" + email;
            String count = redisTemplate.opsForValue().get(key);
            
            if (count == null) {
                redisTemplate.opsForValue().set(key, "1", windowMinutes, TimeUnit.MINUTES);
            } else {
                redisTemplate.opsForValue().increment(key);
            }
        } catch (Exception e) {
            logger.warn("Redis unavailable for rate limit counter update (non-critical): {}", e.getMessage());
        }
    }
    
    private Boolean checkVerificationRateLimit(String email) {
        if (!isRedisAvailable()) {
            logger.debug("Redis unavailable for verification rate limiting, allowing attempt");
            return true; // Allow attempt if Redis is unavailable
        }
        try {
            String key = "otp:verify:rate:" + email;
            String count = redisTemplate.opsForValue().get(key);
            
            if (count == null) {
                return true;
            }
            
            int attemptCount = Integer.parseInt(count);
            return attemptCount < maxVerificationAttempts;
        } catch (Exception e) {
            logger.warn("Redis unavailable for verification rate limiting, allowing attempt: {}", e.getMessage());
            return true; // Allow attempt if Redis is unavailable
        }
    }
    
    private void updateVerificationRateLimitCounter(String email) {
        if (!isRedisAvailable()) {
            return; // Skip if Redis is unavailable
        }
        try {
            String key = "otp:verify:rate:" + email;
            String count = redisTemplate.opsForValue().get(key);
            
            if (count == null) {
                redisTemplate.opsForValue().set(key, "1", verificationWindowSeconds, TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().increment(key);
            }
        } catch (Exception e) {
            logger.warn("Redis unavailable for verification rate limit counter update (non-critical): {}", e.getMessage());
        }
    }
    
    public Integer getResendCooldownRemaining(String email) {
        if (!isRedisAvailable()) {
            return 0; // No cooldown if Redis is unavailable
        }
        try {
            String cooldownKey = "otp:cooldown:" + email;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
                Long remaining = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
                return remaining != null ? remaining.intValue() : 0;
            }
        } catch (Exception e) {
            logger.warn("Redis unavailable for cooldown check: {}", e.getMessage());
        }
        return 0; // No cooldown if Redis is unavailable
    }
}

