package com.travelo.authservice.config;

import com.travelo.authservice.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.OffsetDateTime;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduled-tasks.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledTasksConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasksConfig.class);
    
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    
    public ScheduledTasksConfig(PasswordResetTokenRepository passwordResetTokenRepository) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }
    
    /**
     * Clean up expired password reset tokens daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredPasswordResetTokens() {
        try {
            logger.info("Starting cleanup of expired password reset tokens");
            passwordResetTokenRepository.deleteExpiredTokens(OffsetDateTime.now());
            logger.info("Cleanup of expired password reset tokens completed");
        } catch (Exception e) {
            logger.error("Error during cleanup of expired password reset tokens", e);
        }
    }
}

