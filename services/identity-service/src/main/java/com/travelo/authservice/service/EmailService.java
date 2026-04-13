package com.travelo.authservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    /** Kept wired for when SMTP / OTP email is re-enabled; {@link #sendVerificationOtp} / password reset skip {@code send} for now. */
    @SuppressWarnings("unused")
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.password-reset.email-link-template:https://travelo.com/reset-password?token={token}}")
    private String passwordResetEmailLinkTemplate;
    
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    @Async
    public void sendVerificationOtp(String email, String otp) {
        // TODO: Re-enable mailSender.send when OTP email delivery is configured (spring.mail.*).
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Verify Your Travelo Account");
            message.setText(buildOtpEmailBody(otp));
            // mailSender.send(message);
            logger.info("OTP email send skipped (SMTP off until mail/OTP implemented); to={}", email);
        } catch (Exception e) {
            logger.error("Failed to prepare OTP email for: {}", email, e);
        }
    }
    
    private String buildOtpEmailBody(String otp) {
        return String.format(
            "Welcome to Travelo!\n\n" +
            "Your verification code is: %s\n\n" +
            "This code will expire in 5 minutes.\n\n" +
            "If you didn't request this code, please ignore this email.\n\n" +
            "For security reasons, never share this code with anyone.\n\n" +
            "Best regards,\n" +
            "The Travelo Team",
            otp
        );
    }
    
    @Async
    public void sendPasswordResetEmail(String email, String resetToken) {
        // TODO: Re-enable mailSender.send when transactional email is configured.
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Reset Your Travelo Password");
            message.setText(buildPasswordResetEmailBody(resetToken));
            // mailSender.send(message);
            logger.info("Password reset email send skipped (SMTP off until mail implemented); to={}", email);
        } catch (Exception e) {
            logger.error("Failed to prepare password reset email for: {}", email, e);
        }
    }
    
    private String buildPasswordResetEmailBody(String resetToken) {
        String template = passwordResetEmailLinkTemplate != null && !passwordResetEmailLinkTemplate.isBlank()
                ? passwordResetEmailLinkTemplate
                : "https://travelo.com/reset-password?token={token}";
        String resetUrl = template.contains("{token}")
                ? template.replace("{token}", resetToken)
                : template + (template.contains("?") ? "&" : "?") + "token=" + resetToken;
        
        return String.format(
            "Hello,\n\n" +
            "You requested to reset your password for your Travelo account.\n\n" +
            "Click the link below to reset your password:\n" +
            "%s\n\n" +
            "This link will expire in 1 hour.\n\n" +
            "If you didn't request this password reset, please ignore this email.\n" +
            "Your password will remain unchanged.\n\n" +
            "For security reasons, never share this link with anyone.\n\n" +
            "Best regards,\n" +
            "The Travelo Team",
            resetUrl
        );
    }
}

