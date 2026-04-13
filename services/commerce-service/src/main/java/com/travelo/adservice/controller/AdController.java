package com.travelo.adservice.controller;

import com.travelo.adservice.dto.CreateAdRequest;
import com.travelo.adservice.dto.AdResponse;
import com.travelo.adservice.dto.ErrorResponse;
import com.travelo.adservice.dto.PageResponse;
import com.travelo.adservice.service.AdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns/{campaignId}/ad-groups/{adGroupId}/ads")
@Tag(name = "Ads", description = "Ad management operations")
public class AdController {

    private final AdService adService;

    public AdController(AdService adService) {
        this.adService = adService;
    }

    @GetMapping
    @Operation(summary = "Get all ads", description = "Retrieve a paginated list of ads for an ad group")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved ads",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ad group not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<AdResponse>> getAds(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID campaignId,
            @Parameter(description = "Ad group ID", required = true)
            @PathVariable UUID adGroupId,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(value = "size", defaultValue = "20") int size) {
        
        PageResponse<AdResponse> response = adService.getAds(campaignId, adGroupId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ad by ID", description = "Retrieve a specific ad by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ad found",
                    content = @Content(schema = @Schema(implementation = AdResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ad not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AdResponse> getAdById(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID campaignId,
            @Parameter(description = "Ad group ID", required = true)
            @PathVariable UUID adGroupId,
            @Parameter(description = "Ad ID", required = true)
            @PathVariable UUID id) {
        
        AdResponse response = adService.getAdById(id, campaignId, adGroupId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create a new ad", description = "Create a new ad for an ad group")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ad created successfully",
                    content = @Content(schema = @Schema(implementation = AdResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AdResponse> createAd(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID campaignId,
            @Parameter(description = "Ad group ID", required = true)
            @PathVariable UUID adGroupId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Ad creation request", required = true)
            @Valid @RequestBody CreateAdRequest request) {
        
        AdResponse response = adService.createAd(campaignId, adGroupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update ad", description = "Update an existing ad")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ad updated successfully",
                    content = @Content(schema = @Schema(implementation = AdResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ad not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AdResponse> updateAd(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID campaignId,
            @Parameter(description = "Ad group ID", required = true)
            @PathVariable UUID adGroupId,
            @Parameter(description = "Ad ID", required = true)
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Ad update request", required = true)
            @Valid @RequestBody CreateAdRequest request) {
        
        AdResponse response = adService.updateAd(id, campaignId, adGroupId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete ad", description = "Soft delete an ad")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Ad deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Ad not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteAd(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID campaignId,
            @Parameter(description = "Ad group ID", required = true)
            @PathVariable UUID adGroupId,
            @Parameter(description = "Ad ID", required = true)
            @PathVariable UUID id) {
        
        adService.deleteAd(id, campaignId, adGroupId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "Get ad preview", description = "Get a preview of how the ad will appear in different placements and devices")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Preview generated successfully"),
            @ApiResponse(responseCode = "404", description = "Ad not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Object> getAdPreview(
            @Parameter(description = "Ad ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Placement type (feed, stories, search, etc.)", example = "feed")
            @RequestParam(value = "placement", required = false) String placement,
            @Parameter(description = "Device type (iphone, android)", example = "iphone")
            @RequestParam(value = "device", required = false) String device) {
        
        Object preview = adService.getAdPreview(id, placement, device);
        return ResponseEntity.ok(preview);
    }
}

