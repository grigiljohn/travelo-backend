package com.travelo.authservice.controller;

import com.travelo.authservice.dto.ChangePasswordRequestDTO;
import com.travelo.authservice.dto.DeviceDTO;
import com.travelo.authservice.dto.EmailRequestDTO;
import com.travelo.authservice.dto.FacebookProfilePayload;
import com.travelo.authservice.dto.FacebookSignInRequestDTO;
import com.travelo.authservice.dto.GoogleIdTokenPayload;
import com.travelo.authservice.dto.GoogleSignInRequestDTO;
import com.travelo.authservice.dto.ForgotPasswordRequestDTO;
import com.travelo.authservice.dto.LoginRequestDTO;
import com.travelo.authservice.dto.LoginResponseDTO;
import com.travelo.authservice.dto.OtpResponseDTO;
import com.travelo.authservice.dto.RefreshTokenRequestDTO;
import com.travelo.authservice.dto.ResetPasswordRequestDTO;
import com.travelo.authservice.dto.SignupRequestDTO;
import com.travelo.authservice.dto.SignupResponseDTO;
import com.travelo.authservice.dto.VerifyOtpRequestDTO;
import com.travelo.authservice.dto.VerifyOtpResponseDTO;
import com.travelo.authservice.entity.User;
import com.travelo.authservice.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final UserService userService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final com.travelo.authservice.service.DeviceService deviceService;
    private final com.travelo.authservice.service.PasswordResetService passwordResetService;
    private final com.travelo.authservice.service.LoginAttemptService loginAttemptService;
    private final com.travelo.authservice.service.TokenBlacklistService tokenBlacklistService;
    private final com.travelo.authservice.service.GoogleOAuthService googleOAuthService;
    private final com.travelo.authservice.service.FacebookOAuthService facebookOAuthService;
    
    @Value("${app.jwt.access-token-expiration}")
    private Integer accessTokenExpiration;
    
    @Value("${app.otp.expiration}")
    private Integer otpExpiration;
    
    @Value("${app.otp.resend-cooldown}")
    private Integer resendCooldown;
    
    /**
     * Helper method to check if email is verified from JWT token
     */
    private Boolean isEmailVerifiedFromToken(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return jwtService.extractIsEmailVerified(token);
            }
        } catch (Exception e) {
            logger.warn("Failed to extract email verification status from token", e);
        }
        return false;
    }
    
    /**
     * Helper method to check if email is verified
     * Throws exception if not verified (to be caught and returned as error response)
     */
    private void requireEmailVerification(HttpServletRequest request) {
        Boolean isVerified = isEmailVerifiedFromToken(request);
        if (isVerified == null || !isVerified) {
            throw new RuntimeException("EMAIL_NOT_VERIFIED");
        }
    }
    
    public AuthController(UserService userService, OtpService otpService, 
                         EmailService emailService, JwtService jwtService,
                         PasswordEncoder passwordEncoder,
                         com.travelo.authservice.service.DeviceService deviceService,
                         com.travelo.authservice.service.PasswordResetService passwordResetService,
                         com.travelo.authservice.service.LoginAttemptService loginAttemptService,
                         com.travelo.authservice.service.TokenBlacklistService tokenBlacklistService,
                         com.travelo.authservice.service.GoogleOAuthService googleOAuthService,
                         com.travelo.authservice.service.FacebookOAuthService facebookOAuthService) {
        this.userService = userService;
        this.otpService = otpService;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.deviceService = deviceService;
        this.passwordResetService = passwordResetService;
        this.loginAttemptService = loginAttemptService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.googleOAuthService = googleOAuthService;
        this.facebookOAuthService = facebookOAuthService;
    }
    
    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account, generates JWT tokens, and sends OTP for email verification"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error or duplicate email/username"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/register")
    public ResponseEntity<com.travelo.authservice.dto.ApiResponse<SignupResponseDTO>> register(
            @Valid @RequestBody SignupRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            logger.info("Registration request for email: {}", request.getEmail());
            
            // Create user
            User user = userService.createUser(
                request.getName(),
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getMobile()
            );
            
            // Generate tokens
            String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getIsEmailVerified()
            );
            String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());
            
            // Register device
            com.travelo.authservice.entity.Device device = deviceService.registerOrUpdateDevice(
                user.getId(), 
                httpRequest
            );
            logger.info("Device registered during registration: {} ({})", device.getDeviceId(), device.getDeviceName());
            
            // Generate and send OTP
            String otp = otpService.generateOtp(user.getId(), user.getEmail());
            emailService.sendVerificationOtp(user.getEmail(), otp);
            
            // Build response
            SignupResponseDTO response = new SignupResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getName(),
                user.getIsEmailVerified(),
                accessToken,
                refreshToken,
                accessTokenExpiration
            );
            response.setNeedsOnboarding(true);
            
            logger.info("User registered successfully: {}", user.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(com.travelo.authservice.dto.ApiResponse.success("Account created successfully. Please verify your email.", response));
                    
        } catch (RuntimeException e) {
            logger.error("Registration failed: {}", e.getMessage());
            Map<String, Object> errors = new HashMap<>();
            if (e.getMessage().contains("Email")) {
                errors.put("email", e.getMessage());
            } else if (e.getMessage().contains("Username")) {
                errors.put("username", e.getMessage());
            } else {
                errors.put("error", e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Registration failed", errors));
        } catch (Exception e) {
            logger.error("Unexpected error during registration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Internal server error", "An unexpected error occurred"));
        }
    }
    
    @Operation(
        summary = "Send verification OTP",
        description = "Sends a new OTP code to the user's email for verification. Requires authentication."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP sent successfully"),
        @ApiResponse(responseCode = "400", description = "Email already verified or invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "429", description = "Too many requests - rate limit exceeded")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/send-verification-otp")
    public ResponseEntity<com.travelo.authservice.dto.ApiResponse<OtpResponseDTO>> sendVerificationOtp(@Valid @RequestBody EmailRequestDTO request) {
        try {
            // Get authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.travelo.authservice.dto.ApiResponse.error("Unauthorized. Please login first.", "UNAUTHORIZED"));
            }
            
            String email = request.getEmail();
            User user = userService.findByEmail(email);
            
            if (user.getIsEmailVerified()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(com.travelo.authservice.dto.ApiResponse.error("Email is already verified", "EMAIL_ALREADY_VERIFIED"));
            }
            
            // Check cooldown
            Integer cooldownRemaining = otpService.getResendCooldownRemaining(email);
            if (cooldownRemaining > 0) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(com.travelo.authservice.dto.ApiResponse.error(
                            "Too many OTP requests. Please try again after " + cooldownRemaining + " seconds.",
                            "RATE_LIMIT_EXCEEDED"
                        ));
            }
            
            // Generate and send OTP
            String otp = otpService.generateOtp(user.getId(), email);
            emailService.sendVerificationOtp(email, otp);
            
            OtpResponseDTO response = new OtpResponseDTO(email, otpExpiration, resendCooldown);
            return ResponseEntity.ok(com.travelo.authservice.dto.ApiResponse.success("OTP sent to your email", response));
            
        } catch (RuntimeException e) {
            logger.error("Failed to send OTP: {}", e.getMessage());
            if (e.getMessage().contains("Too many") || e.getMessage().contains("wait")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(com.travelo.authservice.dto.ApiResponse.error(e.getMessage(), "RATE_LIMIT_EXCEEDED"));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(com.travelo.authservice.dto.ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        } catch (Exception e) {
            logger.error("Unexpected error sending OTP", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Internal server error", "An unexpected error occurred"));
        }
    }
    
    @Operation(
        summary = "Verify OTP",
        description = "Verifies the OTP code sent to the user's email and marks the email as verified. Requires authentication."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email verified successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "429", description = "Too many verification attempts")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/verify-otp")
    public ResponseEntity<com.travelo.authservice.dto.ApiResponse<VerifyOtpResponseDTO>> verifyOtp(@Valid @RequestBody VerifyOtpRequestDTO request) {
        try {
            // Get authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.travelo.authservice.dto.ApiResponse.error("Unauthorized. Please login first.", "UNAUTHORIZED"));
            }
            
            String email = request.getEmail();
            String otp = request.getOtp();
            
            // Verify OTP
            Boolean isValid = otpService.verifyOtp(email, otp);
            
            if (!isValid) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(com.travelo.authservice.dto.ApiResponse.error("Invalid or expired OTP", "INVALID_OTP"));
            }
            
            // Update user email verification status
            User user = userService.findByEmail(email);
            user = userService.markEmailAsVerified(user.getId());
            
            // Generate new tokens with updated verification status
            String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getIsEmailVerified()
            );
            String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());
            
            VerifyOtpResponseDTO response = new VerifyOtpResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getIsEmailVerified(),
                accessToken,
                refreshToken
            );
            
            logger.info("Email verified successfully for user: {}", user.getId());
            return ResponseEntity.ok(com.travelo.authservice.dto.ApiResponse.success("Email verified successfully", response));
            
        } catch (RuntimeException e) {
            logger.error("OTP verification failed: {}", e.getMessage());
            if (e.getMessage().contains("expired")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(com.travelo.authservice.dto.ApiResponse.error("OTP has expired", "OTP_EXPIRED"));
            }
            if (e.getMessage().contains("Too many")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(com.travelo.authservice.dto.ApiResponse.error(e.getMessage(), "RATE_LIMIT_EXCEEDED"));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Invalid or expired OTP", "INVALID_OTP"));
        } catch (Exception e) {
            logger.error("Unexpected error verifying OTP", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Internal server error", "An unexpected error occurred"));
        }
    }
    
    @Operation(
        summary = "Resend OTP",
        description = "Resends a new OTP code to the user's email. Subject to cooldown period (60 seconds). Requires authentication."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP resent successfully"),
        @ApiResponse(responseCode = "400", description = "Email already verified or invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "429", description = "Too many requests - cooldown period active")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/resend-otp")
    public ResponseEntity<com.travelo.authservice.dto.ApiResponse<OtpResponseDTO>> resendOtp(@Valid @RequestBody EmailRequestDTO request) {
        try {
            // Get authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.travelo.authservice.dto.ApiResponse.error("Unauthorized. Please login first.", "UNAUTHORIZED"));
            }
            
            String email = request.getEmail();
            User user = userService.findByEmail(email);
            
            if (user.getIsEmailVerified()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(com.travelo.authservice.dto.ApiResponse.error("Email is already verified", "EMAIL_ALREADY_VERIFIED"));
            }
            
            // Check cooldown
            Integer cooldownRemaining = otpService.getResendCooldownRemaining(email);
            if (cooldownRemaining > 0) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(com.travelo.authservice.dto.ApiResponse.error(
                            "Please wait " + cooldownRemaining + " seconds before requesting a new OTP.",
                            "RATE_LIMIT_EXCEEDED"
                        ));
            }
            
            // Generate and send new OTP
            String otp = otpService.generateOtp(user.getId(), email);
            emailService.sendVerificationOtp(email, otp);
            
            OtpResponseDTO response = new OtpResponseDTO(email, otpExpiration, resendCooldown);
            return ResponseEntity.ok(com.travelo.authservice.dto.ApiResponse.success("OTP resent to your email", response));
            
        } catch (RuntimeException e) {
            logger.error("Failed to resend OTP: {}", e.getMessage());
            if (e.getMessage().contains("wait") || e.getMessage().contains("Too many")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(com.travelo.authservice.dto.ApiResponse.error(e.getMessage(), "RATE_LIMIT_EXCEEDED"));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(com.travelo.authservice.dto.ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        } catch (Exception e) {
            logger.error("Unexpected error resending OTP", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Internal server error", "An unexpected error occurred"));
        }
    }
    
    @Operation(
        summary = "Login user",
        description = "Authenticates user with email/username and password, returns JWT tokens"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping("/login")
    public ResponseEntity<com.travelo.authservice.dto.ApiResponse<LoginResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            logger.info("Login request for: {}", request.getEmailOrUsername());
            
            // Check if account is locked
            if (loginAttemptService.isAccountLocked(request.getEmailOrUsername())) {
                Long remainingTime = loginAttemptService.getRemainingLockoutTime(request.getEmailOrUsername());
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(com.travelo.authservice.dto.ApiResponse.error(
                            "Account is temporarily locked due to too many failed login attempts. Please try again after " + 
                            (remainingTime / 60) + " minutes.",
                            "ACCOUNT_LOCKED"
                        ));
            }
            
            // Find user by email or username
            User user = userService.findByEmailOrUsername(request.getEmailOrUsername());
            
            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                // Record failed attempt
                loginAttemptService.recordFailedAttempt(request.getEmailOrUsername());
                
                Integer remainingAttempts = loginAttemptService.getRemainingAttempts(request.getEmailOrUsername());
                String errorMessage = "Invalid email/username or password";
                if (remainingAttempts > 0 && remainingAttempts < 5) {
                    errorMessage += ". " + remainingAttempts + " attempt(s) remaining before account lockout.";
                }
                
                logger.warn("Invalid password attempt for user: {} ({} attempts remaining)", 
                    user.getEmail(), remainingAttempts);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.travelo.authservice.dto.ApiResponse.error(errorMessage, "INVALID_CREDENTIALS"));
            }
            
            // Clear failed attempts on successful login
            loginAttemptService.clearFailedAttempts(request.getEmailOrUsername());
            
            // Register or update device
            com.travelo.authservice.entity.Device device = deviceService.registerOrUpdateDevice(
                user.getId(), 
                httpRequest
            );
            
            // Check if device is new (not trusted)
            boolean isNewDevice = !device.getIsTrusted();
            if (isNewDevice) {
                logger.info("New device detected for user: {} - Device: {}", user.getId(), device.getDeviceName());
            }
            
            // Update last login
            user = userService.updateLastLogin(user.getId());
            
            // Generate tokens
            String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getIsEmailVerified()
            );
            String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());
            
            // Build response
            LoginResponseDTO response = new LoginResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getName(),
                user.getIsEmailVerified(),
                accessToken,
                refreshToken,
                accessTokenExpiration
            );
            
            String message = isNewDevice 
                ? "Login successful. New device detected. Please verify this device." 
                : "Login successful";
            
            logger.info("User logged in successfully: {} from device: {}", user.getId(), device.getDeviceName());
            return ResponseEntity.ok(com.travelo.authservice.dto.ApiResponse.success(message, response));
            
        } catch (RuntimeException e) {
            logger.error("Login failed: {}", e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.travelo.authservice.dto.ApiResponse.error("Invalid email/username or password", "INVALID_CREDENTIALS"));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(com.travelo.authservice.dto.ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        } catch (Exception e) {
            logger.error("Unexpected error during login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Internal server error", "An unexpected error occurred"));
        }
    }

    @Operation(
        summary = "Sign in with Google",
        description = "Verifies a Google ID token, creates an account on first sign-in, returns JWT tokens"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "400", description = "Invalid token or validation error")
    })
    @PostMapping("/google")
    public ResponseEntity<com.travelo.authservice.dto.ApiResponse<LoginResponseDTO>> googleSignIn(
            @Valid @RequestBody GoogleSignInRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            GoogleIdTokenPayload payload = googleOAuthService.verifyAndParse(request.getIdToken());

            Optional<User> existing = userService.findByEmailOptional(payload.getEmail());
            boolean isNew = existing.isEmpty();
            User user;
            if (isNew) {
                user = userService.createUserFromGoogleSignIn(payload.getName(), payload.getEmail());
            } else {
                user = existing.get();
                user = userService.updateLastLogin(user.getId());
            }

            deviceService.registerOrUpdateDevice(user.getId(), httpRequest);

            String accessToken = jwtService.generateAccessToken(
                    user.getId(),
                    user.getEmail(),
                    user.getUsername(),
                    user.getIsEmailVerified()
            );
            String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

            LoginResponseDTO response = new LoginResponseDTO(
                    user.getId(),
                    user.getEmail(),
                    user.getUsername(),
                    user.getName(),
                    user.getIsEmailVerified(),
                    accessToken,
                    refreshToken,
                    accessTokenExpiration
            );
            response.setNeedsOnboarding(isNew);

            String message = isNew ? "Welcome! Your account is ready." : "Login successful";
            logger.info("Google sign-in success for user: {} (new={})", user.getId(), isNew);
            return ResponseEntity.ok(com.travelo.authservice.dto.ApiResponse.success(message, response));
        } catch (RuntimeException e) {
            logger.error("Google sign-in failed: {}", e.getMessage());
            Map<String, Object> errors = new HashMap<>();
            errors.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Google sign-in failed", errors));
        } catch (Exception e) {
            logger.error("Unexpected error during Google sign-in", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Internal server error", "An unexpected error occurred"));
        }
    }

    @Operation(
        summary = "Sign in with Facebook",
        description = "Validates a Facebook user access token via Graph API, creates an account on first sign-in"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "400", description = "Invalid token or validation error")
    })
    @PostMapping("/facebook")
    public ResponseEntity<com.travelo.authservice.dto.ApiResponse<LoginResponseDTO>> facebookSignIn(
            @Valid @RequestBody FacebookSignInRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            FacebookProfilePayload payload = facebookOAuthService.verifyAndParse(request.getAccessToken());

            Optional<User> existing = userService.findByEmailOptional(payload.getEmail());
            boolean isNew = existing.isEmpty();
            User user;
            if (isNew) {
                user = userService.createUserFromGoogleSignIn(payload.getName(), payload.getEmail());
            } else {
                user = existing.get();
                user = userService.updateLastLogin(user.getId());
            }

            deviceService.registerOrUpdateDevice(user.getId(), httpRequest);

            String accessToken = jwtService.generateAccessToken(
                    user.getId(),
                    user.getEmail(),
                    user.getUsername(),
                    user.getIsEmailVerified()
            );
            String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

            LoginResponseDTO response = new LoginResponseDTO(
                    user.getId(),
                    user.getEmail(),
                    user.getUsername(),
                    user.getName(),
                    user.getIsEmailVerified(),
                    accessToken,
                    refreshToken,
                    accessTokenExpiration
            );
            response.setNeedsOnboarding(isNew);

            String message = isNew ? "Welcome! Your account is ready." : "Login successful";
            logger.info("Facebook sign-in success for user: {} (new={})", user.getId(), isNew);
            return ResponseEntity.ok(com.travelo.authservice.dto.ApiResponse.success(message, response));
        } catch (RuntimeException e) {
            logger.error("Facebook sign-in failed: {}", e.getMessage());
            Map<String, Object> errors = new HashMap<>();
            errors.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Facebook sign-in failed", errors));
        } catch (Exception e) {
            logger.error("Unexpected error during Facebook sign-in", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Internal server error", "An unexpected error occurred"));
        }
    }
    
    @Operation(
        summary = "Refresh access token",
        description = "Generates a new access token using a valid refresh token"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh-token")
    public ResponseEntity<com.travelo.authservice.dto.ApiResponse<LoginResponseDTO>> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request) {
        try {
            String refreshToken = request.getRefreshToken();
            
            // Validate refresh token
            if (jwtService.isTokenExpired(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.travelo.authservice.dto.ApiResponse.error("Refresh token has expired", "TOKEN_EXPIRED"));
            }
            
            // Extract user info from token
            String email = jwtService.extractEmail(refreshToken);
            
            // Verify it's a refresh token
            try {
                io.jsonwebtoken.Claims claims = jwtService.extractAllClaimsPublic(refreshToken);
                String tokenType = (String) claims.get("type");
                if (!"refresh".equals(tokenType)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(com.travelo.authservice.dto.ApiResponse.error("Invalid token type", "INVALID_TOKEN"));
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.travelo.authservice.dto.ApiResponse.error("Invalid refresh token", "INVALID_TOKEN"));
            }
            
            // Get user
            User user = userService.findByEmail(email);
            
            // Generate new tokens
            String newAccessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getIsEmailVerified()
            );
            String newRefreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());
            
            // Build response
            LoginResponseDTO response = new LoginResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getName(),
                user.getIsEmailVerified(),
                newAccessToken,
                newRefreshToken,
                accessTokenExpiration
            );
            
            logger.info("Token refreshed for user: {}", user.getId());
            return ResponseEntity.ok(com.travelo.authservice.dto.ApiResponse.success("Token refreshed successfully", response));
            
        } catch (RuntimeException e) {
            logger.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Invalid or expired refresh token", "INVALID_TOKEN"));
        } catch (Exception e) {
            logger.error("Unexpected error during token refresh", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Internal server error", "An unexpected error occurred"));
        }
    }
    
    @Operation(
        summary = "Verify token",
        description = "Validates a JWT access token and returns user information if valid. Used to check authentication status."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token is valid"),
        @ApiResponse(responseCode = "401", description = "Invalid, expired, or blacklisted token")
    })
    // Accept both GET (expected) and POST (some clients call POST) to avoid method errors
    @RequestMapping(value = "/verify", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<com.travelo.authservice.dto.ApiResponse<LoginResponseDTO>> verifyToken(HttpServletRequest request) {
        try {
            // Extract token from Authorization header
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.travelo.authservice.dto.ApiResponse.error("Authorization header missing or invalid", "UNAUTHORIZED"));
            }
            
            String token = authHeader.substring(7);
            
            // Check if token is blacklisted
            if (tokenBlacklistService.isTokenBlacklisted(token)) {
                logger.warn("Blacklisted token attempted to be verified");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.travelo.authservice.dto.ApiResponse.error("Token has been invalidated", "TOKEN_BLACKLISTED"));
            }
            
            // Check if token is expired
            if (jwtService.isTokenExpired(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.travelo.authservice.dto.ApiResponse.error("Token has expired", "TOKEN_EXPIRED"));
            }
            
            // Extract user info from token
            String email = jwtService.extractEmail(token);
            UUID userId = jwtService.extractUserId(token);
            
            if (email == null || userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.travelo.authservice.dto.ApiResponse.error("Invalid token", "INVALID_TOKEN"));
            }
            
            // Get user from database
            User user;
            try {
                user = userService.findByEmail(email);
            } catch (RuntimeException e) {
                logger.warn("User not found for email: {}", email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.travelo.authservice.dto.ApiResponse.error("User not found", "USER_NOT_FOUND"));
            }
            
            // Verify user is not null
            if (user == null) {
                logger.warn("User is null for email: {}", email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.travelo.authservice.dto.ApiResponse.error("User not found", "USER_NOT_FOUND"));
            }
            
            // Verify it's an access token (not refresh token)
            try {
                io.jsonwebtoken.Claims claims = jwtService.extractAllClaimsPublic(token);
                String tokenType = (String) claims.get("type");
                if (tokenType != null && "refresh".equals(tokenType)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(com.travelo.authservice.dto.ApiResponse.error("Refresh token provided. Use access token instead.", "INVALID_TOKEN_TYPE"));
                }
            } catch (Exception e) {
                logger.debug("Could not extract token type, assuming access token", e);
            }
            
            // Build response with user info (with null checks)
            if (user.getId() == null || user.getEmail() == null) {
                logger.error("User has null ID or email: userId={}, email={}", user.getId(), user.getEmail());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(com.travelo.authservice.dto.ApiResponse.error("User data is incomplete", "INCOMPLETE_USER_DATA"));
            }
            
            LoginResponseDTO response = new LoginResponseDTO(
                user.getId(),
                user.getEmail() != null ? user.getEmail() : email, // Fallback to email from token
                user.getUsername() != null ? user.getUsername() : "", // Fallback to empty string
                user.getName() != null ? user.getName() : "", // Fallback to empty string
                user.getIsEmailVerified() != null ? user.getIsEmailVerified() : false, // Fallback to false
                token, // Return the same token
                null,  // No refresh token in verify response
                accessTokenExpiration != null ? accessTokenExpiration : 3600 // Default to 1 hour if not set
            );
            
            logger.debug("Token verified successfully for user: {}", user.getId());
            return ResponseEntity.ok(com.travelo.authservice.dto.ApiResponse.success("Token is valid", response));
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument during token verification: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Invalid token", "INVALID_TOKEN"));
        } catch (org.springframework.dao.DataAccessException e) {
            logger.error("Database error during token verification: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Database connection error. Please try again later.", "DATABASE_ERROR"));
        } catch (RuntimeException e) {
            logger.error("Token verification failed: {}", e.getMessage(), e);
            // Check if it's a database-related error
            if (e.getMessage() != null && (e.getMessage().contains("Connection") || e.getMessage().contains("database") || e.getMessage().contains("SQL"))) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(com.travelo.authservice.dto.ApiResponse.error("Database connection error. Please try again later.", "DATABASE_ERROR"));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Invalid token: " + e.getMessage(), "INVALID_TOKEN"));
        } catch (Exception e) {
            logger.error("Unexpected error during token verification", e);
            String errorMessage = "Internal server error";
            String errorCode = "INTERNAL_ERROR";
            
            // Provide more specific error messages
            if (e.getMessage() != null) {
                if (e.getMessage().contains("Connection") || e.getMessage().contains("database") || e.getMessage().contains("SQL")) {
                    errorMessage = "Database connection error. Please try again later.";
                    errorCode = "DATABASE_ERROR";
                } else if (e.getMessage().contains("timeout")) {
                    errorMessage = "Request timeout. Please try again.";
                    errorCode = "TIMEOUT_ERROR";
                } else {
                    errorMessage = "Internal server error: " + e.getMessage();
                }
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.travelo.authservice.dto.ApiResponse.error(errorMessage, errorCode));
        }
    }
    
    @Operation(
        summary = "Logout user",
        description = "Logs out the current user. In a stateless JWT system, this mainly invalidates the token on client side."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/logout")
    public ResponseEntity<com.travelo.authservice.dto.ApiResponse<Void>> logout(HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                String email = auth.getName();
                
                // Get token from request
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    // Blacklist the token
                    tokenBlacklistService.blacklistToken(token);
                }
                
                logger.info("User logged out: {}", email);
            }
            return ResponseEntity.ok(com.travelo.authservice.dto.ApiResponse.success("Logout successful", null));
        } catch (Exception e) {
            logger.error("Unexpected error during logout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Internal server error", "An unexpected error occurred"));
        }
    }
    
    @Operation(
        summary = "Request password reset",
        description = "Sends a password reset link to the user's email"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password reset email sent (always returns success for security)"),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<com.travelo.authservice.dto.ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        try {
            logger.info("Password reset requested for email: {}", request.getEmail());
            
            // Always return success to prevent email enumeration
            passwordResetService.requestPasswordReset(request.getEmail());
            
            return ResponseEntity.ok(com.travelo.authservice.dto.ApiResponse.success(
                "If an account exists with this email, a password reset link has been sent.",
                null
            ));
        } catch (Exception e) {
            logger.error("Unexpected error during password reset request", e);
            // Still return success to prevent email enumeration
            return ResponseEntity.ok(com.travelo.authservice.dto.ApiResponse.success(
                "If an account exists with this email, a password reset link has been sent.",
                null
            ));
        }
    }
    
    @Operation(
        summary = "Reset password",
        description = "Resets password using a valid reset token"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token"),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<com.travelo.authservice.dto.ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        try {
            logger.info("Password reset attempt with token");
            
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            
            // Blacklist all existing tokens for this user
            // (We'd need to get user from token, but for now token blacklist will handle it via expiration)
            
            return ResponseEntity.ok(com.travelo.authservice.dto.ApiResponse.success("Password reset successfully", null));
            
        } catch (RuntimeException e) {
            logger.error("Password reset failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(com.travelo.authservice.dto.ApiResponse.error(e.getMessage(), "INVALID_TOKEN"));
        } catch (Exception e) {
            logger.error("Unexpected error during password reset", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Internal server error", "An unexpected error occurred"));
        }
    }
    
    @Operation(
        summary = "Change password",
        description = "Changes password for authenticated user. Requires current password."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password changed successfully"),
        @ApiResponse(responseCode = "400", description = "Current password incorrect or validation error"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/change-password")
    public ResponseEntity<com.travelo.authservice.dto.ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDTO request,
            HttpServletRequest httpRequest) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.travelo.authservice.dto.ApiResponse.error("Unauthorized", "UNAUTHORIZED"));
            }
            
            // Check email verification (required for password changes)
            try {
                requireEmailVerification(httpRequest);
            } catch (RuntimeException e) {
                if ("EMAIL_NOT_VERIFIED".equals(e.getMessage())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(com.travelo.authservice.dto.ApiResponse.error(
                                "Email verification required. Please verify your email address before performing this action.",
                                "EMAIL_NOT_VERIFIED"
                            ));
                }
                throw e;
            }
            
            String email = auth.getName();
            User user = userService.findByEmail(email);
            
            // Change password
            userService.changePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword());
            
            // Blacklist all existing tokens (force re-login)
            tokenBlacklistService.blacklistAllUserTokens(email);
            
            logger.info("Password changed for user: {}", user.getId());
            return ResponseEntity.ok(com.travelo.authservice.dto.ApiResponse.success("Password changed successfully", null));
            
        } catch (RuntimeException e) {
            logger.error("Password change failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(com.travelo.authservice.dto.ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        } catch (Exception e) {
            logger.error("Unexpected error during password change", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Internal server error", "An unexpected error occurred"));
        }
    }
    
    @Operation(
        summary = "Deactivate account",
        description = "Deactivates the authenticated user's account"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account deactivated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/deactivate")
    public ResponseEntity<com.travelo.authservice.dto.ApiResponse<Void>> deactivateAccount(HttpServletRequest httpRequest) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.travelo.authservice.dto.ApiResponse.error("Unauthorized", "UNAUTHORIZED"));
            }
            
            // Check email verification (required for account deactivation)
            try {
                requireEmailVerification(httpRequest);
            } catch (RuntimeException e) {
                if ("EMAIL_NOT_VERIFIED".equals(e.getMessage())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(com.travelo.authservice.dto.ApiResponse.error(
                                "Email verification required. Please verify your email address before performing this action.",
                                "EMAIL_NOT_VERIFIED"
                            ));
                }
                throw e;
            }
            
            String email = auth.getName();
            User user = userService.findByEmail(email);
            
            userService.deactivateAccount(user.getId());
            
            // Blacklist all tokens
            tokenBlacklistService.blacklistAllUserTokens(email);
            
            logger.info("Account deactivated for user: {}", user.getId());
            return ResponseEntity.ok(com.travelo.authservice.dto.ApiResponse.success("Account deactivated successfully", null));
            
        } catch (Exception e) {
            logger.error("Unexpected error during account deactivation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Internal server error", "An unexpected error occurred"));
        }
    }
    
    @Operation(
        summary = "Delete account",
        description = "Permanently deletes the authenticated user's account (GDPR compliance)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/account")
    public ResponseEntity<com.travelo.authservice.dto.ApiResponse<Void>> deleteAccount(HttpServletRequest httpRequest) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.travelo.authservice.dto.ApiResponse.error("Unauthorized", "UNAUTHORIZED"));
            }
            
            // Check email verification (required for account deletion)
            try {
                requireEmailVerification(httpRequest);
            } catch (RuntimeException e) {
                if ("EMAIL_NOT_VERIFIED".equals(e.getMessage())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(com.travelo.authservice.dto.ApiResponse.error(
                                "Email verification required. Please verify your email address before performing this action.",
                                "EMAIL_NOT_VERIFIED"
                            ));
                }
                throw e;
            }
            
            String email = auth.getName();
            User user = userService.findByEmail(email);
            
            userService.deleteAccount(user.getId());
            
            // Blacklist all tokens
            tokenBlacklistService.blacklistAllUserTokens(email);
            
            logger.info("Account deleted for user: {}", user.getId());
            return ResponseEntity.ok(com.travelo.authservice.dto.ApiResponse.success("Account deleted successfully", null));
            
        } catch (Exception e) {
            logger.error("Unexpected error during account deletion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Internal server error", "An unexpected error occurred"));
        }
    }
    
    @Operation(
        summary = "Get user devices",
        description = "Returns list of all devices associated with the authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Devices retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/devices")
    public ResponseEntity<com.travelo.authservice.dto.ApiResponse<java.util.List<DeviceDTO>>> getUserDevices() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.travelo.authservice.dto.ApiResponse.error("Unauthorized", "UNAUTHORIZED"));
            }
            
            String email = auth.getName();
            User user = userService.findByEmail(email);
            
            java.util.List<com.travelo.authservice.entity.Device> devices = deviceService.getUserDevices(user.getId());
            
            java.util.List<DeviceDTO> deviceDTOs = devices.stream()
                    .map(device -> new DeviceDTO(
                        device.getId(),
                        device.getDeviceId(),
                        device.getDeviceName(),
                        device.getDeviceType(),
                        device.getIsTrusted(),
                        device.getIpAddress(),
                        device.getLastUsedAt(),
                        device.getCreatedAt()
                    ))
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(com.travelo.authservice.dto.ApiResponse.success("Devices retrieved successfully", deviceDTOs));
            
        } catch (Exception e) {
            logger.error("Error retrieving devices", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Internal server error", "An unexpected error occurred"));
        }
    }
    
    @Operation(
        summary = "Trust device",
        description = "Marks a device as trusted for the authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Device trusted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Device not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/devices/{deviceId}/trust")
    public ResponseEntity<com.travelo.authservice.dto.ApiResponse<Void>> trustDevice(@PathVariable String deviceId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.travelo.authservice.dto.ApiResponse.error("Unauthorized", "UNAUTHORIZED"));
            }
            
            String email = auth.getName();
            User user = userService.findByEmail(email);
            
            deviceService.markDeviceAsTrusted(user.getId(), deviceId);
            
            return ResponseEntity.ok(com.travelo.authservice.dto.ApiResponse.success("Device trusted successfully", null));
            
        } catch (RuntimeException e) {
            logger.error("Error trusting device: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Device not found", "DEVICE_NOT_FOUND"));
        } catch (Exception e) {
            logger.error("Unexpected error trusting device", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Internal server error", "An unexpected error occurred"));
        }
    }
    
    @Operation(
        summary = "Remove device",
        description = "Removes a device from the user's device list"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Device removed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<com.travelo.authservice.dto.ApiResponse<Void>> removeDevice(@PathVariable String deviceId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(com.travelo.authservice.dto.ApiResponse.error("Unauthorized", "UNAUTHORIZED"));
            }
            
            String email = auth.getName();
            User user = userService.findByEmail(email);
            
            deviceService.removeDevice(user.getId(), deviceId);
            
            return ResponseEntity.ok(com.travelo.authservice.dto.ApiResponse.success("Device removed successfully", null));
            
        } catch (Exception e) {
            logger.error("Unexpected error removing device", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(com.travelo.authservice.dto.ApiResponse.error("Internal server error", "An unexpected error occurred"));
        }
    }
}

