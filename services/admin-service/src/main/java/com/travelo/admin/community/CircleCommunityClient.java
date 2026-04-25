package com.travelo.admin.community;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class CircleCommunityClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String socialBaseUrl;

    public CircleCommunityClient(
            RestTemplate mediaRestTemplate,
            ObjectMapper objectMapper,
            @Value("${app.social.base-url:http://localhost:8096}") String socialBaseUrl) {
        this.restTemplate = mediaRestTemplate;
        this.objectMapper = objectMapper;
        this.socialBaseUrl = socialBaseUrl == null ? "http://localhost:8096" : socialBaseUrl.trim();
    }

    public List<AdminCommunityListItem> fetchCommunities(String q) {
        try {
            final String url = socialBaseUrl.replaceAll("/+$", "") + "/api/v1/circles/communities";
            ResponseEntity<String> r = restTemplate.getForEntity(url, String.class);
            if (!r.getStatusCode().is2xxSuccessful() || r.getBody() == null || r.getBody().isBlank()) {
                return List.of();
            }
            JsonNode root = objectMapper.readTree(r.getBody());
            JsonNode data = root.path("data");
            if (!data.isArray()) return List.of();
            String query = q == null ? "" : q.trim().toLowerCase();

            List<AdminCommunityListItem> out = new ArrayList<>();
            for (JsonNode n : data) {
                String circleId = n.path("id").asText("");
                String name = n.path("name").asText("");
                String city = n.path("city").asText("");
                String desc = n.path("description").asText("");
                String visibility = n.path("visibility").asText("public");
                boolean active = true;

                if (!query.isEmpty()) {
                    String hay = (name + " " + city + " " + circleId).toLowerCase();
                    if (!hay.contains(query)) continue;
                }
                long syntheticId = -Math.abs(circleId.hashCode() == Integer.MIN_VALUE ? 1 : circleId.hashCode());
                out.add(new AdminCommunityListItem(
                        syntheticId,
                        circleId,
                        name,
                        desc,
                        n.path("tagline").asText(""),
                        city,
                        n.path("coverImageUrl").asText(""),
                        n.path("iconImageUrl").asText(""),
                        String.join(", ", readStringArray(n.path("tags"))),
                        String.join(", ", readStringArray(n.path("topics"))),
                        n.path("rules").asText(""),
                        n.path("memberCount").asInt(0),
                        0,
                        n.path("requireAdminApproval").asBoolean(false),
                        n.path("allowMemberInvites").asBoolean(true),
                        visibility,
                        active,
                        OffsetDateTime.now(),
                        "circles"
                ));
            }
            return out;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static List<String> readStringArray(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<String> out = new ArrayList<>();
        for (JsonNode item : node) {
            String v = item.asText("").trim();
            if (!v.isEmpty()) out.add(v);
        }
        return out;
    }
}
