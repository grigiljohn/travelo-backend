package com.travelo.adservice.controller;

import com.travelo.adservice.client.MediaServiceClient;
import com.travelo.adservice.client.dto.MediaFileResponse;
import com.travelo.adservice.dto.AssetResponse;
import com.travelo.adservice.dto.ErrorResponse;
import com.travelo.adservice.entity.Asset;
import com.travelo.adservice.entity.enums.AssetType;
import com.travelo.adservice.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/assets")
@Tag(name = "Assets", description = "Asset library management")
public class AssetController {

    private static final Logger logger = LoggerFactory.getLogger(AssetController.class);

    private final MediaServiceClient mediaServiceClient;
    private final AssetService assetService;

    public AssetController(MediaServiceClient mediaServiceClient, AssetService assetService) {
        this.mediaServiceClient = mediaServiceClient;
        this.assetService = assetService;
    }

    @PostMapping("/upload")
    @Operation(
            summary = "Upload media file",
            description = "Upload an image or video file. The file will be saved to S3 via media-service, and an asset record will be created."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "File uploaded successfully",
                    content = @Content(schema = @Schema(implementation = AssetResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AssetResponse> uploadAsset(
            @Parameter(description = "Media file to upload (image or video)", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Business account ID", required = true)
            @RequestParam("businessAccountId") UUID businessAccountId,
            @Parameter(description = "User ID uploading the file", required = true)
            @RequestParam("userId") UUID userId) {

        logger.info("Received file upload request - fileName: {}, size: {} bytes, businessAccountId: {}",
                file.getOriginalFilename(), file.getSize(), businessAccountId);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        // Determine media type from file content type
        String contentType = file.getContentType();
        String mediaType;
        AssetType assetType;

        if (contentType != null && contentType.startsWith("image/")) {
            mediaType = "IMAGE";
            assetType = AssetType.IMAGE;
        } else if (contentType != null && contentType.startsWith("video/")) {
            mediaType = "VIDEO";
            assetType = AssetType.VIDEO;
        } else {
            throw new IllegalArgumentException("Unsupported file type. Only images and videos are allowed.");
        }

        // Upload to media-service (which saves to S3)
        MediaFileResponse mediaFile = mediaServiceClient.uploadMedia(file, mediaType);

        // Create asset record in ad-service
        Asset asset = assetService.createAsset(
                mediaFile.id(),
                mediaFile.fileUrl(),
                assetType,
                businessAccountId,
                userId
        );

        AssetResponse response = AssetResponse.fromEntity(asset);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all assets", description = "Retrieve all assets for a business account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved assets")
    })
    public ResponseEntity<List<AssetResponse>> getAssets(
            @Parameter(description = "Business account ID", required = true)
            @RequestParam("businessAccountId") UUID businessAccountId) {

        List<Asset> assets = assetService.getAssetsByBusinessAccount(businessAccountId);
        List<AssetResponse> responses = assets.stream()
                .map(AssetResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get asset by ID", description = "Retrieve a specific asset by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Asset found",
                    content = @Content(schema = @Schema(implementation = AssetResponse.class))),
            @ApiResponse(responseCode = "404", description = "Asset not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AssetResponse> getAssetById(
            @Parameter(description = "Asset ID", required = true)
            @PathVariable UUID id) {

        Asset asset = assetService.getAssetById(id);
        AssetResponse response = AssetResponse.fromEntity(asset);
        return ResponseEntity.ok(response);
    }
}

