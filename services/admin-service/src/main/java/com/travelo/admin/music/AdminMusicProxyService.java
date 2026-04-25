package com.travelo.admin.music;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelo.admin.music.dto.MusicTrackItem;
import com.travelo.admin.music.dto.MusicUploadResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@Service
public class AdminMusicProxyService {

    private static final Logger log = LoggerFactory.getLogger(AdminMusicProxyService.class);

    private final RestTemplate mediaRestTemplate;
    private final ObjectMapper objectMapper;
    private final String mediaBaseUrl;

    public AdminMusicProxyService(
            RestTemplate mediaRestTemplate,
            ObjectMapper objectMapper,
            @Value("${app.media.base-url:http://localhost:8084}") String mediaBaseUrl) {
        this.mediaRestTemplate = mediaRestTemplate;
        this.objectMapper = objectMapper;
        this.mediaBaseUrl = mediaBaseUrl.replaceAll("/$", "");
    }

    public List<MusicTrackItem> listTracks() {
        String url = mediaBaseUrl + "/api/v1/music";
        try {
            ResponseEntity<String> r = mediaRestTemplate.exchange(url, HttpMethod.GET, null, String.class);
            return objectMapper.readValue(r.getBody(), new TypeReference<>() {});
        } catch (HttpStatusCodeException e) {
            log.warn("Media list music failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Media service: " + e.getStatusCode());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not parse music list");
        }
    }

    public MusicUploadResult upload(
            MultipartFile file,
            MultipartFile thumbnail,
            String name,
            String artist,
            String mood,
            String genre,
            Integer bpm,
            String description,
            Integer durationSeconds,
            boolean isRecommended
    ) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new NamedBytesResource(file.getBytes(), file.getOriginalFilename()));
        if (thumbnail != null && !thumbnail.isEmpty()) {
            body.add(
                    "thumbnail",
                    new NamedBytesResource(thumbnail.getBytes(), thumbnail.getOriginalFilename()));
        }
        if (name != null && !name.isBlank()) {
            body.add("name", name.trim());
        }
        if (artist != null && !artist.isBlank()) {
            body.add("artist", artist.trim());
        }
        if (mood != null && !mood.isBlank()) {
            body.add("mood", mood.trim());
        }
        if (genre != null && !genre.isBlank()) {
            body.add("genre", genre.trim());
        }
        if (bpm != null) {
            body.add("bpm", bpm);
        }
        if (description != null && !description.isBlank()) {
            body.add("description", description.trim());
        }
        if (durationSeconds != null) {
            body.add("durationSeconds", durationSeconds);
        }
        body.add("isRecommended", Boolean.toString(isRecommended));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(body, headers);
        String url = mediaBaseUrl + "/api/v1/music/upload";
        try {
            ResponseEntity<String> r = mediaRestTemplate.postForEntity(url, req, String.class);
            return objectMapper.readValue(r.getBody(), MusicUploadResult.class);
        } catch (HttpStatusCodeException e) {
            String msg = e.getResponseBodyAsString();
            log.warn("Media upload music failed: {} {}", e.getStatusCode(), msg);
            throw new ResponseStatusException(
                    e.getStatusCode().is4xxClientError()
                            ? HttpStatus.BAD_REQUEST
                            : HttpStatus.BAD_GATEWAY,
                    msg != null && !msg.isEmpty() ? msg : "Upload failed");
        }
    }

    private static final class NamedBytesResource extends ByteArrayResource {
        private final String filename;

        NamedBytesResource(byte[] data, String filename) {
            super(data);
            this.filename = filename != null ? filename : "audio.bin";
        }

        @Override
        public String getFilename() {
            return filename;
        }

    }
}
