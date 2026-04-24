package com.travelo.reelservice.client;

import com.travelo.commons.config.ResilientWebClientConfig;
import com.travelo.reelservice.client.dto.ReelProcessMediaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.UUID;

/**
 * Calls media-service reel FFmpeg pipeline.
 */
@Component
public class ReelMediaPipelineClient {

    private static final Logger log = LoggerFactory.getLogger(ReelMediaPipelineClient.class);

    private final WebClient webClient;

    public ReelMediaPipelineClient(
            ResilientWebClientConfig resilientWebClientConfig,
            @Value("${app.media-service.url:http://localhost:8084}") String mediaServiceBaseUrl) {
        // Reuse resilience4j instance "media-service" (see application.yml) — same
        // host as post/story MediaServiceClient; a separate name would require
        // duplicate circuitbreaker + retry entries and caused startup failure
        // when only "media-service" was configured.
        this.webClient = resilientWebClientConfig.createResilientWebClient("media-service", mediaServiceBaseUrl);
    }

    public ReelProcessMediaResponse processReel(MultipartFile file,
                                               UUID ownerId,
                                               String filterType,
                                               boolean musicEnabled,
                                               String clientJobId) throws IOException {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("owner_id", ownerId.toString());
        byte[] bytes = file.getBytes();
        String fn = file.getOriginalFilename() != null ? file.getOriginalFilename() : "reel.mp4";
        String ct = file.getContentType() != null ? file.getContentType() : "video/mp4";
        builder.part("file", new ByteArrayResource(bytes)).filename(fn).contentType(MediaType.parseMediaType(ct));
        builder.part("filter_type", filterType != null ? filterType : "NONE");
        builder.part("music_enabled", Boolean.toString(musicEnabled));
        if (clientJobId != null && !clientJobId.isBlank()) {
            builder.part("client_job_id", clientJobId);
        }

        try {
            return webClient.post()
                    .uri("/v1/media/reel/process-upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(builder.build())
                    .retrieve()
                    .bodyToMono(ReelProcessMediaResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("reel process-upload failed status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Media reel processing failed: " + e.getMessage(), e);
        }
    }
}
