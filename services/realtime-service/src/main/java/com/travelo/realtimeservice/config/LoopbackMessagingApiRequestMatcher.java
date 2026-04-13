package com.travelo.realtimeservice.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Allows messaging REST calls from the same host (e.g. WebSocket layer calling in-process HTTP)
 * without forwarding the end-user JWT.
 */
public final class LoopbackMessagingApiRequestMatcher implements RequestMatcher {

    @Override
    public boolean matches(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod()) && !HttpMethod.POST.matches(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/api/v1/conversations")) {
            return false;
        }
        return LoopbackHttp.isLoopback(request);
    }
}
