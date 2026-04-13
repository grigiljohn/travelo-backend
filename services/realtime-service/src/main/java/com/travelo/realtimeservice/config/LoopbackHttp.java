package com.travelo.realtimeservice.config;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Detects loopback callers (e.g. WebSocket layer posting to in-process REST).
 */
public final class LoopbackHttp {

    private LoopbackHttp() {}

    public static boolean isLoopback(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String addr = request.getRemoteAddr();
        return "127.0.0.1".equals(addr)
                || "::1".equals(addr)
                || "0:0:0:0:0:0:0:1".equals(addr);
    }
}
