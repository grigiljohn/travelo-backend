package com.travelo.adservice.controller;

import com.travelo.adservice.dto.CreateCampaignRequest;
import com.travelo.adservice.dto.CampaignResponse;
import com.travelo.adservice.dto.ErrorResponse;
import com.travelo.adservice.dto.PageResponse;
import com.travelo.adservice.entity.CampaignStatus;
import com.travelo.adservice.service.CampaignService;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns")
@Tag(name = "Campaigns", description = "Campaign management operations")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    @Operation(
            summary = "Get all campaigns",
            description = "Retrieve a paginated list of campaigns with optional filtering by status, objective, and search term"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved campaigns",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<CampaignResponse>> getCampaigns(
            @Parameter(description = "Business account ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @RequestParam(value = "businessAccountId") UUID businessAccountId,
            @Parameter(description = "Filter by campaign status", example = "ACTIVE")
            @RequestParam(value = "status", required = false) CampaignStatus status,
            @Parameter(description = "Search term for campaign name", example = "Summer")
            @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(value = "size", defaultValue = "20") int size,
            @Parameter(description = "Sort field", example = "createdAt")
            @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
            @Parameter(description = "Sort direction (ASC/DESC)", example = "DESC")
            @RequestParam(value = "direction", defaultValue = "DESC") String direction) {
        
        PageResponse<CampaignResponse> response = campaignService.getCampaigns(
                businessAccountId, status, search, page, size, sort, direction);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get campaign by ID", description = "Retrieve a specific campaign by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Campaign found",
                    content = @Content(schema = @Schema(implementation = CampaignResponse.class))),
            @ApiResponse(responseCode = "404", description = "Campaign not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CampaignResponse> getCampaignById(
            @Parameter(description = "Campaign ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id,
            @Parameter(description = "Business account ID", required = true)
            @RequestParam(value = "businessAccountId") UUID businessAccountId) {
        
        CampaignResponse response = campaignService.getCampaignById(id, businessAccountId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create a new campaign", description = "Create a new advertising campaign")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Campaign created successfully",
                    content = @Content(schema = @Schema(implementation = CampaignResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CampaignResponse> createCampaign(
            @Parameter(description = "Business account ID", required = true)
            @RequestParam(value = "businessAccountId") UUID businessAccountId,
            @Parameter(description = "User ID creating the campaign", required = true)
            @RequestParam(value = "userId") UUID userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Campaign creation request", required = true)
            @Valid @RequestBody CreateCampaignRequest request) {
        
        CampaignResponse response = campaignService.createCampaign(businessAccountId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update campaign", description = "Update an existing campaign")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Campaign updated successfully",
                    content = @Content(schema = @Schema(implementation = CampaignResponse.class))),
            @ApiResponse(responseCode = "404", description = "Campaign not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CampaignResponse> updateCampaign(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Business account ID", required = true)
            @RequestParam(value = "businessAccountId") UUID businessAccountId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Campaign update request", required = true)
            @Valid @RequestBody CreateCampaignRequest request) {
        
        CampaignResponse response = campaignService.updateCampaign(id, businessAccountId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete campaign", description = "Soft delete a campaign")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Campaign deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Campaign not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteCampaign(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Business account ID", required = true)
            @RequestParam(value = "businessAccountId") UUID businessAccountId) {
        
        campaignService.deleteCampaign(id, businessAccountId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update campaign status", description = "Update the status of a campaign (ACTIVE, PAUSED, ARCHIVED)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully",
                    content = @Content(schema = @Schema(implementation = CampaignResponse.class))),
            @ApiResponse(responseCode = "404", description = "Campaign not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CampaignResponse> updateCampaignStatus(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Business account ID", required = true)
            @RequestParam(value = "businessAccountId") UUID businessAccountId,
            @Parameter(description = "New campaign status", required = true, example = "ACTIVE")
            @RequestParam(value = "status") CampaignStatus status) {
        
        CampaignResponse response = campaignService.updateCampaignStatus(id, businessAccountId, status);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/bulk")
    @Operation(summary = "Bulk update campaigns", description = "Update multiple campaigns at once")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Campaigns updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> bulkUpdateCampaigns(
            @Parameter(description = "Business account ID", required = true)
            @RequestParam(value = "businessAccountId") UUID businessAccountId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Bulk update request with campaign IDs and updates", required = true)
            @RequestBody Map<String, Object> request) {
        
        @SuppressWarnings("unchecked")
        List<UUID> campaignIds = (List<UUID>) request.get("campaignIds");
        @SuppressWarnings("unchecked")
        Map<String, Object> updates = (Map<String, Object>) request.get("updates");
        
        campaignService.bulkUpdateCampaigns(businessAccountId, campaignIds, updates);
        return ResponseEntity.noContent().build();
    }
}

