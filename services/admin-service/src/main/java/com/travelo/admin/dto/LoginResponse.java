package com.travelo.admin.dto;

import com.travelo.admin.domain.AdminRole;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        String username,
        AdminRole role
) {
}
