package com.travelo.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${app.jwt.secret:${JWT_SECRET:your-256-bit-secret-key-32-bytes!!}}")
    private String jwtSecret;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchange -> exchange
                // Health, docs, auth. User APIs are proxied to identity-service which enforces JWT itself.
                // (Validating the same HS256 token here with OAuth2ResourceServer can reject JJWT-issued
                // access tokens and surface as 403 before the request reaches identity.)
                .pathMatchers(
                        "/actuator/**",
                        // Liveness/readiness via gateway (K8s / LB) without JWT.
                        "/*/actuator/health",
                        "/*/actuator/health/**",
                        "/*/actuator/info",
                        "/auth-service/**",
                        "/user-service/**",
                        // Public read-only catalog (categories, tags) from admin-service; admin JWT is not
                        // available in the mobile app. StripPrefix still forwards to /api/v1/catalog/**.
                        "/admin-service/api/v1/catalog/**",
                        // commerce-service (ad-service route) has no Spring Security; browser SPA uses
                        // this path. Restrict via network / separate gateway in hardening.
                        "/ad-service/**",
                        "/shop-service/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**")
                    .permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtDecoder(reactiveJwtDecoder()))
            );

        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        SecretKey secretKey = new SecretKeySpec(
            jwtSecret.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        );
        // Use HS256 to match auth-service (JJWT defaults to HS256 with Keys.hmacShaKeyFor)
        return NimbusReactiveJwtDecoder
            .withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    }
}


