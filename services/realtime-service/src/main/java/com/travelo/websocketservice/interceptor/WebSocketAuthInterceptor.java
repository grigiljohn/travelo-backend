package com.travelo.websocketservice.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

/**
 * Interceptor to authenticate WebSocket connections using JWT token.
 * Token can be passed as query parameter: ?token=xxx
 * or in Authorization header (if supported by client).
 */
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);
    private final JwtDecoder jwtDecoder;

    public WebSocketAuthInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        try {
            String token = extractToken(request);
            
            if (token == null || token.isEmpty()) {
                logger.warn("WebSocket connection rejected: No token provided");
                response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return false;
            }

            // Validate JWT token
            Jwt jwt = jwtDecoder.decode(token);
            
            // Extract userId from claims (auth-service puts userId in claims, not subject)
            String userIdStr = jwt.getClaimAsString("userId");
            if (userIdStr == null || userIdStr.isEmpty()) {
                logger.warn("WebSocket connection rejected: Invalid token (no userId claim)");
                response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return false;
            }

            // Store user ID in attributes for use in handler
            attributes.put("userId", UUID.fromString(userIdStr));
            attributes.put("token", token);
            
            logger.info("WebSocket connection authenticated for user: {}", userIdStr);
            return true;
            
        } catch (JwtException e) {
            logger.warn("WebSocket connection rejected: Invalid token - {}", e.getMessage());
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        } catch (Exception e) {
            logger.error("Error during WebSocket handshake: {}", e.getMessage(), e);
            response.setStatusCode(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No-op
    }

    private String extractToken(ServerHttpRequest request) {
        // Try query parameter first (most common for WebSocket)
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            
            // Check query parameter
            String token = httpRequest.getParameter("token");
            if (token != null && !token.isEmpty()) {
                return token;
            }
            
            // Check Authorization header
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }
        
        // Try parsing from URI
        URI uri = request.getURI();
        if (uri != null && uri.getQuery() != null) {
            String query = uri.getQuery();
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }
        
        return null;
    }
}

