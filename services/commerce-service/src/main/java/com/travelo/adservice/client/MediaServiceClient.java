package com.travelo.adservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelo.adservice.client.dto.MediaFileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class MediaServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(MediaServiceClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String mediaServiceUrl;

    public MediaServiceClient(RestTemplate restTemplate,
                             ObjectMapper objectMapper,
                             @Value("${media.service.url:http://localhost:8084}") String mediaServiceUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.mediaServiceUrl = mediaServiceUrl;
    }

    /**
     * Upload a media file to media-service, which will save it to S3
     *
     * @param file      The file to upload
     * @param mediaType The type of media (IMAGE or VIDEO)
     * @return MediaFileResponse containing the uploaded file information
     * @throws RuntimeException if upload fails
     */
    public MediaFileResponse uploadMedia(MultipartFile file, String mediaType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        logger.info("Uploading media file to media-service - fileName: {}, contentType: {}, size: {} bytes, mediaType: {}",
                file.getOriginalFilename(), file.getContentType(), file.getSize(), mediaType);

        try {
            // Prepare multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Create ByteArrayResource from MultipartFile
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            
            body.add("file", fileResource);
            body.add("media_type", mediaType);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Call media-service upload endpoint
            String uploadUrl = mediaServiceUrl + "/media/upload";
            logger.debug("Calling media-service at: {}", uploadUrl);

            ResponseEntity<Object> response = restTemplate.postForEntity(
                    uploadUrl,
                    requestEntity,
                    Object.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Convert response to MediaFileResponse
                MediaFileResponse mediaFile = objectMapper.convertValue(response.getBody(), MediaFileResponse.class);
                logger.info("Media file uploaded successfully - id: {}, fileKey: {}, url: {}",
                        mediaFile.id(), mediaFile.fileKey(), mediaFile.fileUrl());
                return mediaFile;
            } else {
                throw new RuntimeException("Failed to upload media file. Status: " + response.getStatusCode());
            }
        } catch (IOException e) {
            logger.error("Error reading file", e);
            throw new RuntimeException("Failed to read file: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error uploading media file to media-service", e);
            throw new RuntimeException("Failed to upload media file to media-service: " + e.getMessage(), e);
        }
    }

    /**
     * Get media file information by ID
     *
     * @param id The media file ID
     * @return MediaFileResponse containing the file information
     */
    public MediaFileResponse getMedia(Long id) {
        logger.debug("Fetching media file from media-service - id: {}", id);

        try {
            String url = mediaServiceUrl + "/media/" + id;
            ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Convert response to MediaFileResponse
                return objectMapper.convertValue(response.getBody(), MediaFileResponse.class);
            } else {
                throw new RuntimeException("Failed to fetch media file. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error fetching media file from media-service", e);
            throw new RuntimeException("Failed to fetch media file from media-service: " + e.getMessage(), e);
        }
    }
}

