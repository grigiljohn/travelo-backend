package com.travelo.authservice.service;

import com.travelo.authservice.entity.PasswordResetToken;
import com.travelo.authservice.entity.User;
import com.travelo.authservice.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class PasswordResetService {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    
    private final PasswordResetTokenRepository tokenRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();
    
    @Value("${app.password-reset.token-expiration:3600}") // 1 hour default
    private Integer tokenExpirationSeconds;
    
    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                               UserService userService,
                               EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userService = userService;
        this.emailService = emailService;
    }
    
    @Transactional
    public void requestPasswordReset(String email) {
        try {
            User user = userService.findByEmail(email);
            
            // Invalidate any existing unused tokens for this user
            tokenRepository.invalidateAllTokensForUser(user.getId());
            
            // Generate secure token
            String token = generateSecureToken();
            
            // Create reset token
            OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(tokenExpirationSeconds);
            PasswordResetToken resetToken = new PasswordResetToken(user.getId(), token, expiresAt);
            tokenRepository.save(resetToken);
            
            // Send password reset email
            emailService.sendPasswordResetEmail(user.getEmail(), token);
            
            logger.info("Password reset token generated for user: {}", user.getId());
        } catch (RuntimeException e) {
            // Don't reveal if email exists or not (security best practice)
            logger.warn("Password reset requested for non-existent email: {}", email);
            // Still return success to prevent email enumeration
        }
    }
    
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));
        
        if (resetToken.getIsUsed()) {
            throw new RuntimeException("Reset token has already been used");
        }
        
        if (resetToken.isExpired()) {
            throw new RuntimeException("Reset token has expired");
        }
        
        // Get user and update password
        User user = userService.findById(resetToken.getUserId());
        userService.updatePassword(user.getId(), newPassword);
        
        // Mark token as used
        resetToken.setIsUsed(true);
        tokenRepository.save(resetToken);
        
        // Invalidate all other tokens for this user
        tokenRepository.invalidateAllTokensForUser(user.getId());
        
        logger.info("Password reset completed for user: {}", user.getId());
    }
    
    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

