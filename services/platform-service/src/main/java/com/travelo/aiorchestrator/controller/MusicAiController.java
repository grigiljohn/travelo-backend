package com.travelo.aiorchestrator.controller;

import com.travelo.aiorchestrator.dto.MusicAutoRecommendRequest;
import com.travelo.aiorchestrator.dto.MusicAutoRecommendResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/music")
@Tag(name = "AI Music", description = "Heuristic AI music recommendation based on mood and tempo")
public class MusicAiController {

    private static final Logger logger = LoggerFactory.getLogger(MusicAiController.class);

    @PostMapping("/recommend")
    @Operation(summary = "Recommend a music track",
            description = "Returns a single heuristically chosen track based on overall mood, tempo and dominant colors. " +
                    "This is a thin wrapper that can later call the dedicated music-service for smarter recommendations.")
    public ResponseEntity<MusicAutoRecommendResponse> recommend(
            @RequestBody MusicAutoRecommendRequest request) {

        String mood = request.overallMood() != null ? request.overallMood() : "neutral";
        String tempo = request.tempo() != null ? request.tempo() : "medium";

        // For now we just echo back a synthetic track; real implementation should call music-service.
        String id = "auto-" + mood + "-" + tempo;
        MusicAutoRecommendResponse response = new MusicAutoRecommendResponse(
                id,
                "AI " + capitalize(mood) + " " + capitalize(tempo),
                "Travelo AI DJ",
                mood,
                switch (tempo) {
                    case "slow" -> 80;
                    case "fast" -> 130;
                    default -> 105;
                },
                request.durationMs() != null ? request.durationMs() : 180_000L,
                null,
                null
        );

        logger.info("AI music recommend: mood={}, tempo={}, colors={}", mood, tempo, request.dominantColors());
        return ResponseEntity.ok(response);
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}

