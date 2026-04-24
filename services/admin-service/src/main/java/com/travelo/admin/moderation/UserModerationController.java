package com.travelo.admin.moderation;

import com.travelo.admin.api.ApiResponse;
import com.travelo.admin.domain.UserSanction;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
public class UserModerationController {
    private final UserSanctionService service;

    public UserModerationController(UserSanctionService service) {
        this.service = service;
    }

    @PostMapping("/{userId}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserSanction>> ban(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok("Banned", service.banUser(userId)));
    }

    @PostMapping("/{userId}/restrict")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserSanction>> restrict(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok("Restricted", service.restrictUser(userId)));
    }
}
