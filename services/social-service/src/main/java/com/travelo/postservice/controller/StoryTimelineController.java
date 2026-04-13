package com.travelo.postservice.controller;

import com.travelo.postservice.dto.ApiResponse;
import com.travelo.postservice.dto.GenerateTimelineRequest;
import com.travelo.postservice.dto.StoryTimelineDto;
import com.travelo.postservice.service.StoryTimelineService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stories/timeline")
public class StoryTimelineController {
    private static final Logger logger = LoggerFactory.getLogger(StoryTimelineController.class);
    private final StoryTimelineService storyTimelineService;

    public StoryTimelineController(StoryTimelineService storyTimelineService) {
        this.storyTimelineService = storyTimelineService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<StoryTimelineDto>> generateTimeline(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody GenerateTimelineRequest request) {
        try {
            String userId = jwt.getSubject();
            logger.info("Generating timeline for user {}", userId);
            StoryTimelineDto timeline = storyTimelineService.generateTimeline(userId, request);
            return ResponseEntity.ok(ApiResponse.success("Timeline generated successfully", timeline));
        } catch (Exception e) {
            logger.error("Error generating timeline", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to generate timeline: " + e.getMessage(), "TIMELINE_GENERATE_FAILED"));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StoryTimelineDto>>> getUserTimelines(
            @AuthenticationPrincipal Jwt jwt) {
        try {
            String userId = jwt.getSubject();
            List<StoryTimelineDto> timelines = storyTimelineService.getUserTimelines(userId);
            return ResponseEntity.ok(ApiResponse.success("Timelines fetched successfully", timelines));
        } catch (Exception e) {
            logger.error("Error fetching timelines", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch timelines: " + e.getMessage(), "TIMELINE_FETCH_FAILED"));
        }
    }
}

