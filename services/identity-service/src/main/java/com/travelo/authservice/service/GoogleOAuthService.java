package com.travelo.authservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelo.authservice.dto.GoogleIdTokenPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoogleOAuthService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuthService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Comma-separated Google OAuth client IDs (Web / Android) allowed in {@code aud} claim.
     * When empty, {@code aud} is not validated (dev only — set in production).
     */
    @Value("${app.oauth.google.allowed-client-ids:}")
    private String allowedClientIds;

    public GoogleIdTokenPayload verifyAndParse(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new RuntimeException("Missing Google ID token");
        }
        URI uri = UriComponentsBuilder
                .fromUriString("https://oauth2.googleapis.com/tokeninfo")
                .queryParam("id_token", idToken)
                .build()
                .toUri();

        ResponseEntity<String> res;
        try {
            res = restTemplate.getForEntity(uri, String.class);
        } catch (Exception e) {
            logger.warn("Google tokeninfo request failed: {}", e.getMessage());
            throw new RuntimeException("Could not verify Google token");
        }
        if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
            throw new RuntimeException("Invalid Google token");
        }
        try {
            JsonNode j = objectMapper.readTree(res.getBody());
            if (j.has("error")) {
                throw new RuntimeException("Invalid Google token");
            }
            String email = text(j, "email");
            if (!StringUtils.hasText(email)) {
                throw new RuntimeException("Google token has no email");
            }
            boolean verified = parseBool(j, "email_verified");
            if (!verified) {
                throw new RuntimeException("Google email is not verified");
            }
            String aud = text(j, "aud");
            validateAudience(aud);

            String name = text(j, "name");
            if (!StringUtils.hasText(name)) {
                name = email.contains("@") ? email.substring(0, email.indexOf('@')) : "Traveler";
            }
            return new GoogleIdTokenPayload(email.trim(), name.trim(), true);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Failed to parse Google tokeninfo: {}", e.getMessage());
            throw new RuntimeException("Invalid Google token");
        }
    }

    private void validateAudience(String aud) {
        if (!StringUtils.hasText(allowedClientIds)) {
            logger.warn("app.oauth.google.allowed-client-ids is empty — skipping Google aud validation");
            return;
        }
        List<String> allowed = Arrays.stream(allowedClientIds.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        if (allowed.isEmpty()) {
            return;
        }
        if (!allowed.contains(aud)) {
            throw new RuntimeException("Google token audience is not allowed");
        }
    }

    private static String text(JsonNode j, String field) {
        JsonNode n = j.get(field);
        if (n == null || n.isNull()) {
            return "";
        }
        return n.asText("");
    }

    private static boolean parseBool(JsonNode j, String field) {
        JsonNode n = j.get(field);
        if (n == null || n.isNull()) {
            return false;
        }
        if (n.isBoolean()) {
            return n.booleanValue();
        }
        return "true".equalsIgnoreCase(n.asText());
    }
}
