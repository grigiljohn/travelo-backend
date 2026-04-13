package com.travelo.postservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Security configuration for Post service.
 * Validates JWT using the same HMAC HS256 secret as auth-service and API gateway,
 * so SecurityUtils.getCurrentUserIdAsString() works for create post, like, save, etc.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.jwt.secret:${JWT_SECRET:your-256-bit-secret-key-32-bytes!!}}")
    private String jwtSecret;

    /**
     * Read-mostly moments traffic without JWT parsing so invalid Bearer does not block
     * {@code GET /feed}, {@code GET /{id}}, etc. Engagement writes ({@code POST} like, comment, view)
     * must <strong>not</strong> use this chain — they need the default chain so the JWT is parsed
     * and {@code SecurityUtils} / {@code resolveUserId} see the signed-in user.
     */
    @Bean
    @Order(0)
    public SecurityFilterChain momentsAnonFriendlyReadsChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(new OrRequestMatcher(
                new AntPathRequestMatcher("/api/v1/moments/feed", "GET"),
                new AntPathRequestMatcher("/api/v1/moments", "POST"),
                new AntPathRequestMatcher("/api/v1/moments/**", "GET")
            ))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/health", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                // Dev-safe read access so feed-service can aggregate posts without JWT.
                .requestMatchers(HttpMethod.GET, "/api/v1/posts", "/api/v1/posts/**").permitAll()
                // Former standalone services had no JWT; keep same openness inside social-service.
                .requestMatchers("/api/v1/stories/**").permitAll()
                .requestMatchers("/api/v1/feed/**").permitAll()
                .requestMatchers("/api/v1/reels/**").permitAll()
                .requestMatchers("/api/v1/moments/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/plans", "/api/v1/plans/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/search/unified").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/circles/discovery").permitAll()
                .requestMatchers("/api/v1/circles/communities", "/api/v1/circles/communities/**")
                    .permitAll()
                .requestMatchers("/api/v1/ai/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey secretKey = new SecretKeySpec(
            jwtSecret.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        );
        return NimbusJwtDecoder
            .withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    }
}
