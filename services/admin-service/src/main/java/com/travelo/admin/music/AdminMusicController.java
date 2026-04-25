package com.travelo.admin.music;

import com.travelo.admin.api.ApiResponse;
import com.travelo.admin.music.dto.MusicTrackItem;
import com.travelo.admin.music.dto.MusicUploadResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/admin/music")
public class AdminMusicController {

    private final AdminMusicProxyService musicProxy;

    public AdminMusicController(AdminMusicProxyService musicProxy) {
        this.musicProxy = musicProxy;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MusicTrackItem>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(musicProxy.listTracks()));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<ApiResponse<MusicUploadResult>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "artist", required = false) String artist,
            @RequestParam(value = "mood", required = false) String mood,
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam(value = "bpm", required = false) Integer bpm,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds,
            @RequestParam(value = "isRecommended", defaultValue = "false") boolean isRecommended
    ) throws IOException {
        MusicUploadResult r = musicProxy.upload(
                file, thumbnail, name, artist, mood, genre, bpm, description, durationSeconds, isRecommended);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Uploaded", r));
    }
}
