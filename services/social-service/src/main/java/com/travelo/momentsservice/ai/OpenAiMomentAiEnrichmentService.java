package com.travelo.momentsservice.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelo.momentsservice.config.MomentsAiOpenAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Optional OpenAI Chat Completions call for moment AI suggest. On any failure or when disabled, returns empty.
 * Timeline fields (scenes, segmentsJson, highlightsJson) are always computed on-device in {@link MomentsServiceImpl}.
 */
@Service
public class OpenAiMomentAiEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiMomentAiEnrichmentService.class);

    private static final Set<String> ALLOWED_FILTERS = Set.of(
            "Original", "Warm", "Cinematic", "Vibrant", "B&W", "Cool"
    );

    private final MomentsAiOpenAiProperties props;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public OpenAiMomentAiEnrichmentService(
            MomentsAiOpenAiProperties props,
            ObjectMapper objectMapper,
            WebClient.Builder webClientBuilder
    ) {
        this.props = props;
        this.objectMapper = objectMapper;
        String base = props.getBaseUrl() == null || props.getBaseUrl().isBlank()
                ? "https://api.openai.com/v1"
                : props.getBaseUrl().trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.webClient = webClientBuilder.baseUrl(base).build();
    }

    public Optional<MomentAiLlmCaptionPack> maybeEnrich(MomentAiLlmRequest request) {
        if (!props.isEnabled() || !StringUtils.hasText(props.getApiKey())) {
            return Optional.empty();
        }
        try {
            String prompt = buildUserPrompt(request);
            Map<String, Object> body = buildChatRequest(prompt);
            String raw = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + props.getApiKey().trim())
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(Math.max(3, props.getTimeoutSeconds())))
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.warn("flow=moment_ai_openai http status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
                        return Mono.empty();
                    })
                    .block();
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }
            return parseAssistantJson(raw);
        } catch (Exception e) {
            log.warn("flow=moment_ai_openai failed: {}", e.toString());
            return Optional.empty();
        }
    }

    private Map<String, Object> buildChatRequest(String userPrompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.getModel());
        body.put("temperature", 0.65);
        body.put("max_tokens", Math.max(120, props.getMaxTokens()));
        body.put("response_format", Map.of("type", "json_object"));
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", "You help edit short travel/social video moments. Reply with a single JSON object only, no markdown."
        ));
        messages.add(Map.of("role", "user", "content", userPrompt));
        body.put("messages", messages);
        return body;
    }

    private static String buildUserPrompt(MomentAiLlmRequest r) {
        String tags = r.parsedTags() == null || r.parsedTags().isEmpty()
                ? "(none)"
                : String.join(", ", r.parsedTags());
        String cap = r.userCaption() == null ? "" : r.userCaption().trim();
        String loc = r.location() == null ? "" : r.location().trim();
        return """
                Context for a mobile "moment" editor:
                - action (pipeline step): %s
                - userCaption (may be empty): %s
                - location (may be empty): %s
                - existingTags: %s
                - approximateClipDurationSec: %.1f

                Return JSON with exactly these keys:
                "caption" (string, one engaging line, max 220 chars, no hashtags),
                "tags" (array of 2-6 short english tokens, lowercase, no #),
                "videoFilter" (exactly one of: Original, Warm, Cinematic, Vibrant, B&W, Cool).

                Match the action: for "scenes" prefer Original filter; for "music-sync" prefer Vibrant; for "smart-trim" or "studio_suggest" prefer Warm; for "highlights" prefer Cinematic unless the caption clearly needs another look.
                """.formatted(r.normalizedAction(), cap, loc, tags, r.durationSec());
    }

    private Optional<MomentAiLlmCaptionPack> parseAssistantJson(String openAiResponseBody) throws Exception {
        JsonNode root = objectMapper.readTree(openAiResponseBody);
        String content = root.path("choices").path(0).path("message").path("content").asText(null);
        if (!StringUtils.hasText(content)) {
            return Optional.empty();
        }
        JsonNode payload = objectMapper.readTree(content.trim());
        String caption = payload.path("caption").asText("").trim();
        if (caption.length() > 500) {
            caption = caption.substring(0, 500);
        }
        if (!StringUtils.hasText(caption)) {
            return Optional.empty();
        }
        List<String> tags = new ArrayList<>();
        if (payload.path("tags").isArray()) {
            for (JsonNode t : payload.path("tags")) {
                String s = t.asText("").trim().toLowerCase(Locale.ROOT);
                if (StringUtils.hasText(s) && tags.size() < 12) {
                    tags.add(s.replace('#', ' ').trim());
                }
            }
        }
        String filterRaw = payload.path("videoFilter").asText("").trim();
        String filter = ALLOWED_FILTERS.contains(filterRaw) ? filterRaw : "Cinematic";

        return Optional.of(new MomentAiLlmCaptionPack(caption, tags, filter));
    }
}
