package com.travelo.admin.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelo.admin.media.dto.AdminImageUploadResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class AdminMediaUploadProxyService {

    private static final Logger log = LoggerFactory.getLogger(AdminMediaUploadProxyService.class);
    private static final UUID FALLBACK_OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final RestTemplate mediaRestTemplate;
    private final ObjectMapper objectMapper;
    private final String mediaBaseUrl;

    public AdminMediaUploadProxyService(
            RestTemplate mediaRestTemplate,
            ObjectMapper objectMapper,
            @Value("${app.media.base-url:http://localhost:8084}") String mediaBaseUrl
    ) {
        this.mediaRestTemplate = mediaRestTemplate;
        this.objectMapper = objectMapper;
        this.mediaBaseUrl = mediaBaseUrl.replaceAll("/$", "");
    }

    public AdminImageUploadResult uploadImage(MultipartFile file, String ownerHint) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");
        }
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("owner_id", resolveOwnerId(ownerHint).toString());
        body.add("media_type", "image");
        body.add("filename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg");
        body.add("file", new NamedBytesResource(file.getBytes(), file.getOriginalFilename()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(body, headers);
        String url = mediaBaseUrl + "/v1/media/upload";
        try {
            String res = mediaRestTemplate.postForObject(url, req, String.class);
            JsonNode root = objectMapper.readTree(res);
            String downloadUrl = root.path("downloadUrl").asText("");
            String storageKey = root.path("storageKey").asText("");
            String idRaw = root.path("mediaId").asText("");
            if (downloadUrl.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Media upload returned no download URL");
            }
            return new AdminImageUploadResult(
                    idRaw.isBlank() ? null : UUID.fromString(idRaw),
                    downloadUrl,
                    storageKey.isBlank() ? null : storageKey
            );
        } catch (HttpStatusCodeException e) {
            String msg = e.getResponseBodyAsString();
            log.warn("Admin image upload failed: {} {}", e.getStatusCode(), msg);
            throw new ResponseStatusException(
                    e.getStatusCode().is4xxClientError() ? HttpStatus.BAD_REQUEST : HttpStatus.BAD_GATEWAY,
                    msg != null && !msg.isBlank() ? msg : "Image upload failed");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Image upload failed");
        }
    }

    private UUID resolveOwnerId(String ownerHint) {
        if (ownerHint == null || ownerHint.isBlank()) return FALLBACK_OWNER;
        try {
            return UUID.fromString(ownerHint.trim());
        } catch (Exception ignore) {
            return UUID.nameUUIDFromBytes(ownerHint.trim().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static final class NamedBytesResource extends ByteArrayResource {
        private final String filename;

        NamedBytesResource(byte[] data, String filename) {
            super(data);
            this.filename = filename != null && !filename.isBlank() ? filename : "image.jpg";
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
