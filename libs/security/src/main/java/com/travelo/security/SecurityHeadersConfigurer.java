package com.travelo.security;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * Shared security defaults for Travelo services.
 */
public final class SecurityHeadersConfigurer {

    private SecurityHeadersConfigurer() {
    }

    public static Customizer<HttpSecurity> defaults() {
        return http -> {
            try {
                http
                        .csrf(csrf -> csrf.disable())
                        .headers(headers -> headers
                                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                                .frameOptions(frame -> frame.sameOrigin())
                        )
                        .authorizeHttpRequests(auth -> auth
                                .requestMatchers("/health", "/actuator/**").permitAll()
                                .anyRequest().authenticated()
                        );
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to configure security", ex);
            }
        };
    }
}
