package com.travelo.admin.domain;

import org.springframework.security.core.GrantedAuthority;

public enum AdminRole {
    ADMIN,
    MODERATOR;

    public GrantedAuthority authority() {
        return () -> "ROLE_" + name();
    }
}
