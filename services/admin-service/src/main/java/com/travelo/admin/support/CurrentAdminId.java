package com.travelo.admin.support;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

public final class CurrentAdminId {
    private CurrentAdminId() {}

    public static String asString(Authentication a) {
        if (a == null || a.getPrincipal() == null) {
            return "anonymous";
        }
        @SuppressWarnings("unchecked")
        var p = (Map<String, Object>) a.getPrincipal();
        return p.get("id") != null ? p.get("id").toString() : "0";
    }

    public static String fromContext() {
        return asString(SecurityContextHolder.getContext().getAuthentication());
    }
}
