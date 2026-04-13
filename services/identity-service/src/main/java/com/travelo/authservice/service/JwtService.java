package com.travelo.authservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {
    
    @Value("${app.jwt.secret}")
    private String secret;
    
    @Value("${app.jwt.access-token-expiration}")
    private Long accessTokenExpiration;
    
    @Value("${app.jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    
    public String generateAccessToken(UUID userId, String email, String username, Boolean isEmailVerified) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("email", email);
        claims.put("username", username);
        claims.put("isEmailVerified", isEmailVerified);
        claims.put("type", "access");
        
        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(accessTokenExpiration, ChronoUnit.SECONDS)))
                .signWith(getSigningKey())
                .compact();
    }
    
    public String generateRefreshToken(UUID userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("email", email);
        claims.put("type", "refresh");
        
        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(refreshTokenExpiration, ChronoUnit.SECONDS)))
                .signWith(getSigningKey())
                .compact();
    }
    
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public UUID extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        String userIdStr = (String) claims.get("userId");
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }
    
    public Boolean extractIsEmailVerified(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("isEmailVerified", Boolean.class);
    }
    
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    public Boolean validateToken(String token, String email) {
        try {
            final String tokenEmail = extractEmail(token);
            return (tokenEmail.equals(email) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }
    
    // Public method to extract all claims
    public Claims extractAllClaimsPublic(String token) {
        return extractAllClaims(token);
    }
}

