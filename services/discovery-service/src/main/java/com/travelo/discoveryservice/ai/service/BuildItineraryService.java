package com.travelo.discoveryservice.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelo.discoveryservice.ai.config.OpenAiProperties;
import com.travelo.discoveryservice.ai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a structured itinerary via OpenAI (JSON) or template fallback.
 */
@Service
public class BuildItineraryService {

    private static final Logger log = LoggerFactory.getLogger(BuildItineraryService.class);

    private static final String SYSTEM = """
            You are an expert travel planner. Reply with ONLY a single JSON object (no markdown), using this exact shape:
            {
              "title": "string — short trip title",
              "summary": "string — 2-4 sentences overview",
              "days": [
                {
                  "dayNumber": 1,
                  "dayTitle": "string",
                  "items": [
                    {
                      "time": "9:00 AM",
                      "title": "string",
                      "description": "string — concrete, place-agnostic if unknown venues",
                      "durationAtSpot": "2h",
                      "icon": "walk|food|flight|beach|museum|nightlife|shopping|hiking|optional",
                      "travelToNext": { "distance": "3 km", "duration": "12 min" }
                    }
                  ]
                }
              ]
            }
            Use 2-4 items per day. Omit travelToNext on the last item of each day. dayNumber must start at 1 and increment.
            """;

    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;
    private final WebClient openAiWebClient;
    private final ItineraryTemplateFallbackService fallback;

    public BuildItineraryService(
            OpenAiProperties openAiProperties,
            ObjectMapper objectMapper,
            @Qualifier("openAiWebClient") WebClient openAiWebClient,
            ItineraryTemplateFallbackService fallback) {
        this.openAiProperties = openAiProperties;
        this.objectMapper = objectMapper;
        this.openAiWebClient = openAiWebClient;
        this.fallback = fallback;
    }

    public BuildItineraryResponse build(BuildItineraryRequest request) {
        if (request.destination() == null || request.destination().isBlank()) {
            throw new IllegalArgumentException("destination is required");
        }
        if (!openAiProperties.isEnabled()
                || openAiProperties.getApiKey() == null
                || openAiProperties.getApiKey().isBlank()) {
            log.info("OpenAI disabled or no API key — using template itinerary");
            return fallback.build(request, "template");
        }
        try {
            return callOpenAi(request);
        } catch (Exception e) {
            log.warn("OpenAI itinerary failed, using template: {}", e.getMessage());
            return fallback.build(request, "template");
        }
    }

    private BuildItineraryResponse callOpenAi(BuildItineraryRequest r) {
        int days = r.durationDays() != null ? r.durationDays() : 5;
        days = Math.min(14, Math.max(1, days));

        String user = buildUserPrompt(r, days);
        Map<String, Object> body = new HashMap<>();
        body.put("model", openAiProperties.getModel());
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM),
                Map.of("role", "user", "content", user)
        ));

        String raw;
        try {
            raw = openAiWebClient
                    .post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + openAiProperties.getApiKey().trim())
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("OpenAI HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
        if (raw == null || raw.isEmpty()) {
            throw new IllegalStateException("empty OpenAI response");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new IllegalStateException("invalid OpenAI JSON envelope", e);
        }
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            throw new IllegalStateException("no assistant content");
        }
        String json = content.asText();
        final BuildItineraryLlmJson plan;
        try {
            plan = objectMapper.readValue(json, BuildItineraryLlmJson.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("model output is not valid itinerary JSON", e);
        }
        if (plan.days() == null || plan.days().isEmpty()) {
            throw new IllegalStateException("no days in model output");
        }
        return new BuildItineraryResponse(
                plan.title() != null ? plan.title() : "Trip plan",
                plan.summary() != null ? plan.summary() : "",
                "openai",
                normalizeDays(plan.days(), days, r.destination().trim())
        );
    }

    private String buildUserPrompt(BuildItineraryRequest r, int dayCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("Create a ").append(dayCount).append("-day travel itinerary for: ").append(r.destination().trim())
                .append(".\n");
        if (r.dateSummary() != null && !r.dateSummary().isBlank()) {
            sb.append("Date context: ").append(r.dateSummary().trim()).append("\n");
        }
        if (r.budget() != null && !r.budget().isBlank()) {
            sb.append("Budget style: ").append(r.budget().trim());
            if (r.customBudgetUsd() != null) {
                sb.append(" (approx USD ").append(r.customBudgetUsd()).append(")");
            }
            sb.append("\n");
        }
        if (r.companions() != null && !r.companions().isEmpty()) {
            sb.append("Party: ").append(String.join(", ", r.companions())).append("\n");
        }
        if (r.activities() != null && !r.activities().isEmpty()) {
            sb.append("Must include interests / activities: ")
                    .append(String.join(", ", r.activities()))
                    .append("\n");
        }
        sb.append("Return exactly ").append(dayCount).append(" days in the JSON \"days\" array.");
        return sb.toString();
    }

    private List<BuildItineraryDayPayload> normalizeDays(
            List<BuildItineraryDayPayload> days, int expectedCount, String destination) {
        var out = new ArrayList<BuildItineraryDayPayload>();
        int n = Math.min(days.size(), expectedCount);
        for (int i = 0; i < n; i++) {
            BuildItineraryDayPayload d = days.get(i);
            int dayNum = d.dayNumber() > 0 ? d.dayNumber() : i + 1;
            String title = d.dayTitle() != null && !d.dayTitle().isBlank()
                    ? d.dayTitle()
                    : "Day " + dayNum + " — " + destination;
            List<BuildItineraryItemPayload> items = d.items() != null ? d.items() : List.of();
            out.add(new BuildItineraryDayPayload(dayNum, title, items));
        }
        while (out.size() < expectedCount) {
            int next = out.size() + 1;
            out.add(new BuildItineraryDayPayload(
                    next,
                    "Day " + next + " — " + destination,
                    List.of(new BuildItineraryItemPayload(
                            "10:00 AM",
                            "Explore",
                            "More time in " + destination + " — add stops you like.",
                            "3h",
                            "walk",
                            null
                    ))
            ));
        }
        return out;
    }
}
