package com.travelo.postservice.service.impl;

import com.travelo.postservice.dto.MediaUploadResponse;
import com.travelo.postservice.entity.MediaUpload;
import com.travelo.postservice.entity.enums.MediaType;
import com.travelo.postservice.repository.MediaUploadRepository;
import com.travelo.postservice.service.MediaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Transactional
public class MediaServiceImpl implements MediaService {

    private static final Logger logger = LoggerFactory.getLogger(MediaServiceImpl.class);
    private final MediaUploadRepository mediaUploadRepository;

    @Value("${app.media.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.media.base-url:http://localhost:8083}")
    private String baseUrl;

    private static final long MAX_IMAGE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final long MAX_VIDEO_SIZE = 500 * 1024 * 1024; // 500MB
    private static final String DEFAULT_SYSTEM_USER = "system-user";

    public MediaServiceImpl(MediaUploadRepository mediaUploadRepository) {
        this.mediaUploadRepository = mediaUploadRepository;
        logger.info("MediaServiceImpl initialized - uploadDir: {}, baseUrl: {}", uploadDir, baseUrl);
    }

    @Override
    public MediaUploadResponse uploadMedia(MultipartFile file, String mediaType) {
        logger.debug("Starting media upload - mediaType: {}, fileName: {}, fileSize: {} bytes", 
                mediaType, file.getOriginalFilename(), file.getSize());
        
        // Validate file
        if (file.isEmpty()) {
            logger.warn("Empty file upload attempt - mediaType: {}", mediaType);
            throw new IllegalArgumentException("File is empty");
        }

        // Validate media type
        if (!"image".equalsIgnoreCase(mediaType) && !"video".equalsIgnoreCase(mediaType)) {
            logger.warn("Invalid media type - mediaType: {}", mediaType);
            throw new IllegalArgumentException("Invalid media type. Must be 'image' or 'video'");
        }

        // Validate file size
        long maxSize = "image".equalsIgnoreCase(mediaType) ? MAX_IMAGE_SIZE : MAX_VIDEO_SIZE;
        if (file.getSize() > maxSize) {
            logger.warn("File size exceeds limit - fileSize: {} bytes, maxSize: {} bytes", 
                    file.getSize(), maxSize);
            throw new IllegalArgumentException(
                String.format("File size exceeds maximum allowed size of %dMB", maxSize / (1024 * 1024))
            );
        }

        // Validate file type
        String contentType = file.getContentType();
        logger.debug("File contentType: {}", contentType);
        if (!isValidMediaType(contentType, mediaType)) {
            logger.warn("Invalid file type - contentType: {}, expectedMediaType: {}", 
                    contentType, mediaType);
            throw new IllegalArgumentException("Invalid file type. Only images and videos are allowed.");
        }

        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                logger.debug("Creating upload directory: {}", uploadPath);
                Files.createDirectories(uploadPath);
                logger.info("Created upload directory: {}", uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
            String filename = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(filename);
            logger.debug("Generated filename: {} for original: {}", filename, originalFilename);

            // Save file
            logger.debug("Saving file to: {}", filePath);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            logger.debug("File saved successfully to: {}", filePath);

            // Generate public URL
            String url = baseUrl + "/uploads/" + filename;
            logger.debug("Generated public URL: {}", url);

            // Extract metadata (simplified - in production, use libraries like ImageIO, FFmpeg, etc.)
            logger.debug("Extracting metadata for file: {}", filePath);
            MediaMetadata metadata = extractMetadata(filePath, mediaType, contentType);

            // Save to database
            String mediaUploadId = UUID.randomUUID().toString();
            logger.debug("Creating media upload record - mediaUploadId: {}", mediaUploadId);
            MediaUpload mediaUpload = new MediaUpload(
                mediaUploadId,
                DEFAULT_SYSTEM_USER,
                url,
                MediaType.valueOf(mediaType.toUpperCase())
            );
            mediaUpload.setFileSize(file.getSize());
            mediaUpload.setWidth(metadata.getWidth());
            mediaUpload.setHeight(metadata.getHeight());
            mediaUpload.setDuration(metadata.getDuration());
            mediaUpload.setThumbnailUrl(metadata.getThumbnailUrl());

            mediaUpload = mediaUploadRepository.save(mediaUpload);
            logger.debug("Media upload saved to database - mediaUploadId: {}", mediaUploadId);

            // Build response
            MediaUploadResponse response = new MediaUploadResponse();
            response.setUrl(url);
            response.setMediaId(mediaUploadId);
            response.setMediaType(mediaType.toLowerCase());
            response.setFileSize(file.getSize());
            response.setWidth(metadata.getWidth());
            response.setHeight(metadata.getHeight());
            response.setDuration(metadata.getDuration());
            response.setThumbnailUrl(metadata.getThumbnailUrl());
            response.setUploadedAt(OffsetDateTime.now().toString());

            logger.info("Media uploaded successfully - mediaId: {}, mediaType: {}, fileSize: {} bytes, url: {}", 
                    mediaUploadId, mediaType, file.getSize(), url);
            return response;
        } catch (IOException e) {
            logger.error("IO error during media upload - mediaType: {}", mediaType, e);
            throw new RuntimeException("Failed to upload media: " + e.getMessage(), e);
        }
    }

    private boolean isValidMediaType(String contentType, String mediaType) {
        if (contentType == null) {
            logger.debug("Content type is null");
            return false;
        }

        boolean isValid;
        if ("image".equalsIgnoreCase(mediaType)) {
            isValid = contentType.startsWith("image/") &&
                (contentType.equals("image/jpeg") ||
                 contentType.equals("image/png") ||
                 contentType.equals("image/gif") ||
                 contentType.equals("image/webp"));
        } else if ("video".equalsIgnoreCase(mediaType)) {
            isValid = contentType.startsWith("video/") &&
                (contentType.equals("video/mp4") ||
                 contentType.equals("video/quicktime") ||
                 contentType.equals("video/x-msvideo") ||
                 contentType.equals("video/webm"));
        } else {
            isValid = false;
        }

        logger.debug("Media type validation - contentType: {}, mediaType: {}, isValid: {}", 
                contentType, mediaType, isValid);
        return isValid;
    }

    private MediaMetadata extractMetadata(Path filePath, String mediaType, String contentType) {
        logger.debug("Extracting metadata - filePath: {}, mediaType: {}, contentType: {}", 
                filePath, mediaType, contentType);
        // Simplified metadata extraction
        // In production, use proper libraries:
        // - For images: ImageIO, BufferedImage
        // - For videos: FFmpeg, JAVE, etc.
        
        MediaMetadata metadata = new MediaMetadata();
        
        // For now, return null values - these should be extracted using proper libraries
        // TODO: Implement proper metadata extraction
        metadata.setWidth(null);
        metadata.setHeight(null);
        metadata.setDuration(null);
        metadata.setThumbnailUrl(null);

        logger.debug("Metadata extraction completed - width: {}, height: {}, duration: {}", 
                metadata.getWidth(), metadata.getHeight(), metadata.getDuration());
        return metadata;
    }

    private static class MediaMetadata {
        private Integer width;
        private Integer height;
        private Integer duration;
        private String thumbnailUrl;

        public Integer getWidth() {
            return width;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }

        public Integer getHeight() {
            return height;
        }

        public void setHeight(Integer height) {
            this.height = height;
        }

        public Integer getDuration() {
            return duration;
        }

        public void setDuration(Integer duration) {
            this.duration = duration;
        }

        public String getThumbnailUrl() {
            return thumbnailUrl;
        }

        public void setThumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
        }
    }
}

