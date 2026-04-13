package com.travelo.postservice.controller;

import com.travelo.postservice.dto.ApiResponse;
import com.travelo.postservice.dto.AudioLibraryDto;
import com.travelo.postservice.service.AudioLibraryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audio")
public class AudioLibraryController {
    private static final Logger logger = LoggerFactory.getLogger(AudioLibraryController.class);
    private final AudioLibraryService audioLibraryService;

    public AudioLibraryController(AudioLibraryService audioLibraryService) {
        this.audioLibraryService = audioLibraryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AudioLibraryDto>>> getAllAudio(
            @RequestParam(required = false) String category) {
        try {
            List<AudioLibraryDto> audio = category != null 
                ? audioLibraryService.getAudioByCategory(category)
                : audioLibraryService.getAllAudio();
            return ResponseEntity.ok(ApiResponse.success("Audio library fetched successfully", audio));
        } catch (Exception e) {
            logger.error("Error fetching audio library", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch audio library: " + e.getMessage(), "AUDIO_FETCH_FAILED"));
        }
    }
}

