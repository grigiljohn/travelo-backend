package com.travelo.admin.auth;

import com.travelo.admin.api.ApiResponse;
import com.travelo.admin.domain.AdminUser;
import com.travelo.admin.dto.LoginRequest;
import com.travelo.admin.dto.LoginResponse;
import com.travelo.admin.repository.AdminUserRepository;
import com.travelo.admin.security.AdminJwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/admin/auth")
public class AuthController {
    private final AdminUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AdminJwtService jwt;
    @Value("${app.admin.jwt.expiration-ms}")
    private long expMs;

    public AuthController(AdminUserRepository users, PasswordEncoder passwordEncoder, AdminJwtService jwt) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest req) {
        AdminUser u = users.findByUsernameIgnoreCase(req.username().trim())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");
        }
        var token = jwt.createToken(u);
        return ResponseEntity.ok(ApiResponse.ok("OK", new LoginResponse(
                token,
                "Bearer",
                expMs,
                u.getUsername(),
                u.getRole()
        )));
    }
}
