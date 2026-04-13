package com.travelo.musicservice.service;

import com.travelo.musicservice.config.MusicAwsProperties;
import com.travelo.musicservice.dto.MusicTrackResponse;
import com.travelo.musicservice.dto.MusicUploadResponse;
import com.travelo.musicservice.entity.MusicTrack;
import com.travelo.musicservice.repository.MusicTrackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MusicService {

    private static final Logger log = LoggerFactory.getLogger(MusicService.class);

    private final MusicTrackRepository repository;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final MusicAwsProperties awsProperties;

    public MusicService(
            MusicTrackRepository repository,
            @Qualifier("musicS3Client") S3Client s3Client,
            @Qualifier("musicS3Presigner") S3Presigner s3Presigner,
            MusicAwsProperties awsProperties) {
        this.repository = repository;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.awsProperties = awsProperties;
    }

    /**
     * Get recommended music tracks
     */
    @Transactional(readOnly = true)
    public List<MusicTrackResponse> getRecommended() {
        log.debug("Fetching recommended music tracks");
        List<MusicTrack> tracks = repository.findByIsRecommendedTrueAndIsActiveTrueOrderByPlayCountDesc();
        return tracks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get music tracks by mood
     */
    @Transactional(readOnly = true)
    public List<MusicTrackResponse> getByMood(String mood) {
        log.debug("Fetching music tracks for mood: {}", mood);
        List<MusicTrack> tracks = repository.findByMoodAndIsActiveTrueOrderByPlayCountDesc(mood);
        return tracks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Search music tracks
     */
    @Transactional(readOnly = true)
    public List<MusicTrackResponse> search(String query) {
        log.debug("Searching music tracks with query: {}", query);
        List<MusicTrack> tracks = repository.searchTracks(query);
        return tracks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all active music tracks
     */
    @Transactional(readOnly = true)
    public List<MusicTrackResponse> getAll() {
        log.debug("Fetching all active music tracks");
        List<MusicTrack> tracks = repository.findByIsActiveTrueOrderByPlayCountDesc();
        return tracks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Upload audio (and optional cover image) to S3, then persist a {@link MusicTrack}.
     */
    @Transactional
    public com.travelo.musicservice.dto.MusicUploadResponse uploadToS3(
            MultipartFile file,
            MultipartFile thumbnail,
            String name,
            String artist,
            String mood,
            Integer durationSeconds
    ) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Audio file is required");
        }
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        if (!isAllowedAudio(contentType, file.getOriginalFilename())) {
            throw new IllegalArgumentException("Unsupported audio type: " + contentType);
        }

        UUID trackId = UUID.randomUUID();
        String safeAudioName = sanitizeFilename(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "track.mp3"
        );
        String fileKey = awsProperties.getUploadPrefix().replaceAll("/$", "")
                + "/" + trackId + "/" + safeAudioName;

        log.info("Uploading music to S3: bucket={}, key={}, size={}", awsProperties.getBucket(), fileKey, file.getSize());
        PutObjectRequest audioPut = PutObjectRequest.builder()
                .bucket(awsProperties.getBucket())
                .key(fileKey)
                .contentType(contentType)
                .contentLength(file.getSize())
                .build();
        try (var in = file.getInputStream()) {
            s3Client.putObject(audioPut, RequestBody.fromInputStream(in, file.getSize()));
        }
        log.info("S3 upload complete: key={}", fileKey);

        String thumbnailKey = null;
        if (thumbnail != null && !thumbnail.isEmpty()) {
            String thumbCt = thumbnail.getContentType() != null ? thumbnail.getContentType() : "image/jpeg";
            if (!thumbCt.startsWith("image/")) {
                throw new IllegalArgumentException("Thumbnail must be an image");
            }
            String safeThumb = sanitizeFilename(
                    thumbnail.getOriginalFilename() != null ? thumbnail.getOriginalFilename() : "cover.jpg"
            );
            thumbnailKey = awsProperties.getThumbnailPrefix().replaceAll("/$", "")
                    + "/" + trackId + "/" + safeThumb;
            PutObjectRequest thumbPut = PutObjectRequest.builder()
                    .bucket(awsProperties.getBucket())
                    .key(thumbnailKey)
                    .contentType(thumbCt)
                    .contentLength(thumbnail.getSize())
                    .build();
            try (var tin = thumbnail.getInputStream()) {
                s3Client.putObject(thumbPut, RequestBody.fromInputStream(tin, thumbnail.getSize()));
            }
            log.info("S3 thumbnail upload complete: key={}", thumbnailKey);
        }

        String trackName = (name != null && !name.isBlank())
                ? name.trim()
                : baseName(safeAudioName);
        String trackArtist = (artist != null && !artist.isBlank()) ? artist.trim() : "Unknown";

        MusicTrack track = new MusicTrack();
        track.setId(trackId);
        track.setName(trackName);
        track.setArtist(trackArtist);
        track.setMood(mood != null && !mood.isBlank() ? mood.trim() : null);
        track.setDurationSeconds(durationSeconds);
        track.setFileKey(fileKey);
        track.setThumbnailKey(thumbnailKey);
        track.setIsRecommended(false);
        track.setIsActive(true);
        track.setPlayCount(0L);
        track = repository.save(track);

        return new MusicUploadResponse(
                track.getId(),
                track.getFileKey(),
                track.getThumbnailKey(),
                track.getName(),
                track.getArtist(),
                track.getMood(),
                track.getDurationSeconds(),
                generatePresignedUrl(track.getThumbnailKey()),
                generatePresignedUrl(track.getFileKey()),
                track.getIsRecommended()
        );
    }

    private static boolean isAllowedAudio(String contentType, String originalFilename) {
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("audio/")) {
            return true;
        }
        String lower = originalFilename != null ? originalFilename.toLowerCase(Locale.ROOT) : "";
        return lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".aac")
                || lower.endsWith(".wav") || lower.endsWith(".flac") || lower.endsWith(".ogg")
                || lower.endsWith(".opus") || lower.endsWith(".webm");
    }

    private static String sanitizeFilename(String name) {
        String base = name.replace("\\", "/");
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        return base.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String baseName(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    /**
     * Convert entity to response DTO with presigned URLs
     */
    private MusicTrackResponse toResponse(MusicTrack track) {
        String thumbnailUrl = generatePresignedUrl(track.getThumbnailKey());
        String fileUrl = generatePresignedUrl(track.getFileKey());

        return new MusicTrackResponse(
                track.getId(),
                track.getName(),
                track.getArtist(),
                track.getMood(),
                track.getDurationSeconds(),
                thumbnailUrl,
                fileUrl,
                track.getIsRecommended()
        );
    }

    /**
     * Generate presigned URL for S3 object
     */
    private String generatePresignedUrl(String s3Key) {
        if (s3Key == null || s3Key.isEmpty()) {
            return null;
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(awsProperties.getBucket())
                    .key(s3Key)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(
                    presignerRequest -> presignerRequest
                            .signatureDuration(Duration.ofMinutes(awsProperties.getPresignExpiryMinutes()))
                            .getObjectRequest(getObjectRequest)
            );

            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Error generating presigned URL for key: {}", s3Key, e);
            return null;
        }
    }
}

