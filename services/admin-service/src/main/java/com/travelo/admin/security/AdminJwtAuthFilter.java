package com.travelo.admin.security;

import com.travelo.admin.domain.AdminRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class AdminJwtAuthFilter extends OncePerRequestFilter {
    private static final String BEARER = "Bearer ";
    private final AdminJwtService jwtService;

    public AdminJwtAuthFilter(AdminJwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws IOException, jakarta.servlet.ServletException {
        String path = request.getRequestURI();
        if (!path.startsWith("/admin/") || path.startsWith("/admin/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }
        String h = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (h == null || !h.startsWith(BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = h.substring(BEARER.length());
        try {
            Claims c = jwtService.parse(token);
            long userId = Long.parseLong(c.getSubject());
            String roleName = c.get("role", String.class);
            var role = roleName != null ? AdminRole.valueOf(roleName) : AdminRole.MODERATOR;
            var auth = new UsernamePasswordAuthenticationToken(
                    Map.of("id", userId, "username", c.get("username", String.class)),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (ExpiredJwtException | MalformedJwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
}
