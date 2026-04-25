package com.travelo.musicservice.controller;

import com.travelo.musicservice.dto.MusicTrackResponse;
import com.travelo.musicservice.dto.MusicUploadResponse;
import com.travelo.musicservice.service.MusicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/music")
@Tag(name = "Music", description = "Music track management APIs")
public class MusicController {

    private final MusicService musicService;

    public MusicController(MusicService musicService) {
        this.musicService = musicService;
    }

    @GetMapping("/recommended")
    @Operation(summary = "Get recommended music tracks", description = "Returns a list of recommended music tracks")
    public ResponseEntity<List<MusicTrackResponse>> getRecommended() {
        List<MusicTrackResponse> tracks = musicService.getRecommended();
        return ResponseEntity.ok(tracks);
    }

    @GetMapping("/mood/{mood}")
    @Operation(summary = "Get music tracks by mood", description = "Returns music tracks filtered by mood (chill, romantic, energetic, calm, happy, etc.)")
    public ResponseEntity<List<MusicTrackResponse>> getByMood(@PathVariable String mood) {
        List<MusicTrackResponse> tracks = musicService.getByMood(mood);
        return ResponseEntity.ok(tracks);
    }

    @GetMapping("/search")
    @Operation(summary = "Search music tracks", description = "Search music tracks by name or artist")
    public ResponseEntity<List<MusicTrackResponse>> search(
            @RequestParam(required = false, defaultValue = "") String q) {
        List<MusicTrackResponse> tracks = musicService.search(q);
        return ResponseEntity.ok(tracks);
    }

    @GetMapping
    @Operation(summary = "Get all music tracks", description = "Returns all active music tracks")
    public ResponseEntity<List<MusicTrackResponse>> getAll() {
        List<MusicTrackResponse> tracks = musicService.getAll();
        return ResponseEntity.ok(tracks);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload music to S3",
            description = "Uploads an audio file to the configured S3 bucket (music.aws.*), optionally a cover image, "
                    + "then saves track metadata. Returns presigned GET URLs for playback."
    )
    public ResponseEntity<MusicUploadResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "artist", required = false) String artist,
            @RequestParam(value = "mood", required = false) String mood,
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam(value = "bpm", required = false) Integer bpm,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds,
            @RequestParam(value = "isRecommended", required = false, defaultValue = "false") boolean isRecommended
    ) throws IOException {
        MusicUploadResponse body = musicService.uploadToS3(
                file, thumbnail, name, artist, mood, genre, bpm, description, durationSeconds, isRecommended);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}

