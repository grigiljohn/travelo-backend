package com.travelo.aiorchestrator.controller;

import com.travelo.aiorchestrator.dto.AutoCutTimelineRequest;
import com.travelo.aiorchestrator.dto.AutoCutTimelineResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/internal/auto-cut")
@Tag(name = "AI AutoCut", description = "Heuristic AutoCut timeline generation")
public class AutoCutController {

    private static final Logger logger = LoggerFactory.getLogger(AutoCutController.class);

    @PostMapping("/timeline")
    @Operation(summary = "Generate AutoCut timeline",
            description = "Generates a simple heuristic story timeline (order, durations, transitions) " +
                    "for the given media IDs. This is intentionally lightweight and can be replaced with " +
                    "a more advanced ML-based implementation later.")
    public ResponseEntity<AutoCutTimelineResponse> generateTimeline(
            @Validated @RequestBody AutoCutTimelineRequest request) {

        List<String> mediaIds = new ArrayList<>(request.mediaIds());
        if (mediaIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Basic heuristic: keep original order, vary duration slightly around a base value
        long targetDurationMs = Optional.ofNullable(request.targetDurationMs())
                .orElse(mediaIds.size() * 3_000L);
        int basePerClip = (int) Math.max(1_500, targetDurationMs / mediaIds.size());

        Map<String, Integer> durations = new LinkedHashMap<>();
        Random random = new Random();
        for (String id : mediaIds) {
            int jitter = random.nextInt(1_000) - 500; // ±500ms
            durations.put(id, Math.max(1_500, basePerClip + jitter));
        }

        // Simple transitions: alternate fade / slide
        List<Map<String, Object>> transitions = new ArrayList<>();
        String[] types = new String[]{"fade", "slide_right", "slide_left", "zoom_in"};
        for (int i = 0; i < mediaIds.size() - 1; i++) {
            Map<String, Object> t = new HashMap<>();
            t.put("from", i);
            t.put("to", i + 1);
            t.put("type", types[i % types.length]);
            t.put("duration", 500);
            transitions.add(t);
        }

        Map<String, String> labels = new HashMap<>();
        labels.put("mood", "neutral");
        labels.put("source", "ai-orchestrator-heuristic");

        AutoCutTimelineResponse response = new AutoCutTimelineResponse(
                mediaIds,
                durations,
                transitions,
                labels
        );

        logger.info("Generated AutoCut timeline for {} media items", mediaIds.size());
        return ResponseEntity.ok(response);
    }
}

