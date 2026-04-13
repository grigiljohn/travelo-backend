package com.travelo.platformservice.analytics;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Ingest endpoint used by social-service (reels). Legacy analytics-service had no controller; this accepts events.
 */
@RestController
@RequestMapping("/api/v1")
@Hidden
public class AnalyticsEventsController {

    @PostMapping("/events")
    public ResponseEntity<Void> ingest(@RequestBody Map<String, Object> event) {
        return ResponseEntity.accepted().build();
    }
}
