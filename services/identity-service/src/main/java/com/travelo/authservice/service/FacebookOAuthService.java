package com.travelo.authservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelo.authservice.dto.FacebookProfilePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class FacebookOAuthService {

    private static final Logger logger = LoggerFactory.getLogger(FacebookOAuthService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validates a Facebook user access token and loads email + name from Graph API.
     */
    public FacebookProfilePayload verifyAndParse(String userAccessToken) {
        if (!StringUtils.hasText(userAccessToken)) {
            throw new RuntimeException("Missing Facebook access token");
        }
        URI uri = UriComponentsBuilder
                .fromUriString("https://graph.facebook.com/v21.0/me")
                .queryParam("fields", "id,name,email")
                .queryParam("access_token", userAccessToken.trim())
                .build()
                .toUri();

        ResponseEntity<String> res;
        try {
            res = restTemplate.getForEntity(uri, String.class);
        } catch (Exception e) {
            logger.warn("Facebook Graph request failed: {}", e.getMessage());
            throw new RuntimeException("Could not verify Facebook token");
        }
        if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
            throw new RuntimeException("Invalid Facebook token");
        }
        try {
            JsonNode j = objectMapper.readTree(res.getBody());
            if (j.has("error")) {
                String msg = j.path("error").path("message").asText("Invalid token");
                throw new RuntimeException(msg);
            }
            String email = text(j, "email");
            if (!StringUtils.hasText(email)) {
                throw new RuntimeException(
                        "Facebook did not return an email. Grant email permission or use email sign-in.");
            }
            String name = text(j, "name");
            if (!StringUtils.hasText(name)) {
                name = email.contains("@") ? email.substring(0, email.indexOf('@')) : "Traveler";
            }
            return new FacebookProfilePayload(email.trim(), name.trim());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Failed to parse Facebook Graph response: {}", e.getMessage());
            throw new RuntimeException("Invalid Facebook response");
        }
    }

    private static String text(JsonNode j, String field) {
        JsonNode n = j.get(field);
        if (n == null || n.isNull()) {
            return "";
        }
        return n.asText("");
    }
}
