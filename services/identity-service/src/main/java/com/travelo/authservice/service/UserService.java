package com.travelo.authservice.service;

import com.travelo.authservice.entity.User;
import com.travelo.authservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Transactional
    public User createUser(String name, String username, String email, String password, String mobile) {
        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already registered");
        }
        
        // Check if username already exists
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username is already taken");
        }
        
        // Hash password
        String hashedPassword = passwordEncoder.encode(password);
        
        // Create user
        User user = new User(name, username, email, hashedPassword, mobile);
        user = userRepository.save(user);
        
        logger.info("User created successfully with ID: {}", user.getId());
        return user;
    }
    
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    public Optional<User> findByEmailOptional(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Creates a user from a verified Google account (email pre-verified, random password hash).
     */
    @Transactional
    public User createUserFromGoogleSignIn(String name, String email) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already registered");
        }
        String localPart = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String username = allocateUsernameFromEmailLocal(localPart);
        String hashedPassword = passwordEncoder.encode(UUID.randomUUID().toString());
        User user = new User(name, username, email, hashedPassword, null);
        user.setIsEmailVerified(true);
        user = userRepository.save(user);
        logger.info("Google OAuth user created: {}", user.getId());
        return user;
    }

    private String allocateUsernameFromEmailLocal(String rawLocal) {
        String base = rawLocal.replaceAll("[^a-zA-Z0-9_]", "");
        if (base.length() < 3) {
            base = "usr";
        }
        if (base.length() > 24) {
            base = base.substring(0, 24);
        }
        String candidate = base;
        int n = 0;
        while (userRepository.existsByUsername(candidate)) {
            n++;
            String suffix = "_" + n;
            int maxBase = Math.max(3, 30 - suffix.length());
            candidate = base.substring(0, Math.min(base.length(), maxBase)) + suffix;
            if (n > 5000) {
                throw new RuntimeException("Could not allocate username");
            }
        }
        return candidate.length() > 30 ? candidate.substring(0, 30) : candidate;
    }
    
    public User findById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }
    
    @Transactional
    public User markEmailAsVerified(UUID userId) {
        User user = findById(userId);
        user.setIsEmailVerified(true);
        user = userRepository.save(user);
        logger.info("Email verified for user: {}", userId);
        return user;
    }
    
    public Boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public Boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
    
    public User findByEmailOrUsername(String emailOrUsername) {
        // Try email first
        return userRepository.findByEmail(emailOrUsername)
                .orElseGet(() -> {
                    // If not found, try username
                    return userRepository.findByUsername(emailOrUsername)
                            .orElseThrow(() -> new RuntimeException("User not found with email or username: " + emailOrUsername));
                });
    }
    
    @Transactional
    public User updateLastLogin(UUID userId) {
        User user = findById(userId);
        user.setLastLoginAt(java.time.OffsetDateTime.now());
        user = userRepository.save(user);
        logger.info("Last login updated for user: {}", userId);
        return user;
    }
    
    @Transactional
    public User updatePassword(UUID userId, String newPassword) {
        User user = findById(userId);
        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(hashedPassword);
        user = userRepository.save(user);
        logger.info("Password updated for user: {}", userId);
        return user;
    }
    
    @Transactional
    public User changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = findById(userId);
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        // Check if new password is same as current
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("New password must be different from current password");
        }
        
        // Update password
        return updatePassword(userId, newPassword);
    }
    
    @Transactional
    public User deactivateAccount(UUID userId) {
        User user = findById(userId);
        // In a real system, you might have an isActive field
        // For now, we'll just log it
        logger.info("Account deactivation requested for user: {}", userId);
        return user;
    }
    
    @Transactional
    public void deleteAccount(UUID userId) {
        User user = findById(userId);
        userRepository.delete(user);
        logger.info("Account deleted for user: {}", userId);
    }
}

