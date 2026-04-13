package com.travelo.mediaservice.controller;

import com.travelo.commons.HealthResponse;
import com.travelo.mediaservice.entity.MediaStatus;
import com.travelo.mediaservice.repository.MediaFileRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class MediaServiceHealthController {

    private final MediaFileRepository mediaFileRepository;

    public MediaServiceHealthController(MediaFileRepository mediaFileRepository) {
        this.mediaFileRepository = mediaFileRepository;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        for (MediaStatus status : MediaStatus.values()) {
            response.put(status.name().toLowerCase(), mediaFileRepository.countByState(status));
        }
        Map<String, Object> payload = HealthResponse.ok("media-service");
        payload.put("media", response);
        return payload;
    }
}
