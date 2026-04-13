package com.travelo.authservice.config;

import com.travelo.authservice.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private com.travelo.authservice.service.TokenBlacklistService tokenBlacklistService;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/google",
                               "/api/v1/auth/facebook", "/api/v1/auth/refresh-token", "/api/v1/auth/forgot-password", 
                               "/api/v1/auth/reset-password", "/api/v1/auth/verify").permitAll()
                .requestMatchers("/health").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }
    
    public class JwtAuthenticationFilter extends OncePerRequestFilter {

        private void writeUnauthorized(HttpServletResponse response, String code) throws IOException {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"" + code + "\"}");
        }

        /** Do not hard-fail JWT on auth routes (e.g. refresh may send an expired access token in Authorization). */
        private boolean isPublicAuthPath(HttpServletRequest request) {
            String uri = request.getRequestURI();
            return uri.contains("/api/v1/auth/");
        }
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, 
                                      HttpServletResponse response, 
                                      FilterChain filterChain) throws ServletException, IOException {
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }
            
            String token = authHeader.substring(7).trim();
            if (token.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            final boolean publicAuth = isPublicAuthPath(request);
            
            try {
                if (tokenBlacklistService.isTokenBlacklisted(token)) {
                    logger.warn("Blacklisted token attempted to be used");
                    if (publicAuth) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                    writeUnauthorized(response, "TOKEN_BLACKLISTED");
                    return;
                }

                if (jwtService.isTokenExpired(token)) {
                    if (publicAuth) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                    writeUnauthorized(response, "TOKEN_EXPIRED");
                    return;
                }

                io.jsonwebtoken.Claims claims = jwtService.extractAllClaimsPublic(token);
                String typ = claims.get("type", String.class);
                if (typ != null && !"access".equals(typ)) {
                    if (publicAuth) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                    writeUnauthorized(response, "WRONG_TOKEN_TYPE");
                    return;
                }

                String email = jwtService.extractEmail(token);
                UUID userId = jwtService.extractUserId(token);

                if (email == null || userId == null) {
                    if (publicAuth) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                    writeUnauthorized(response, "INVALID_TOKEN");
                    return;
                }

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                logger.error("JWT authentication failed", e);
                if (publicAuth) {
                    filterChain.doFilter(request, response);
                    return;
                }
                writeUnauthorized(response, "INVALID_TOKEN");
                return;
            }
            
            filterChain.doFilter(request, response);
        }
    }
}

