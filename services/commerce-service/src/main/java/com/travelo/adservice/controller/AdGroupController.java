package com.travelo.adservice.controller;

import com.travelo.adservice.dto.CreateAdGroupRequest;
import com.travelo.adservice.dto.AdGroupResponse;
import com.travelo.adservice.dto.ErrorResponse;
import com.travelo.adservice.dto.PageResponse;
import com.travelo.adservice.service.AdGroupService;
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
@RequestMapping("/api/v1/campaigns/{campaignId}/ad-groups")
@Tag(name = "Ad Groups", description = "Ad group (ad-set) management operations")
public class AdGroupController {

    private final AdGroupService adGroupService;

    public AdGroupController(AdGroupService adGroupService) {
        this.adGroupService = adGroupService;
    }

    @GetMapping
    @Operation(summary = "Get all ad groups", description = "Retrieve a paginated list of ad groups for a campaign")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved ad groups",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "404", description = "Campaign not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PageResponse<AdGroupResponse>> getAdGroups(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID campaignId,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(value = "size", defaultValue = "20") int size) {
        
        PageResponse<AdGroupResponse> response = adGroupService.getAdGroups(campaignId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ad group by ID", description = "Retrieve a specific ad group by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ad group found",
                    content = @Content(schema = @Schema(implementation = AdGroupResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ad group not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AdGroupResponse> getAdGroupById(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID campaignId,
            @Parameter(description = "Ad group ID", required = true)
            @PathVariable UUID id) {
        
        AdGroupResponse response = adGroupService.getAdGroupById(id, campaignId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create a new ad group", description = "Create a new ad group for a campaign")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Ad group created successfully",
                    content = @Content(schema = @Schema(implementation = AdGroupResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AdGroupResponse> createAdGroup(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID campaignId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Ad group creation request", required = true)
            @Valid @RequestBody CreateAdGroupRequest request) {
        
        AdGroupResponse response = adGroupService.createAdGroup(campaignId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update ad group", description = "Update an existing ad group")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ad group updated successfully",
                    content = @Content(schema = @Schema(implementation = AdGroupResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ad group not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AdGroupResponse> updateAdGroup(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID campaignId,
            @Parameter(description = "Ad group ID", required = true)
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Ad group update request", required = true)
            @Valid @RequestBody CreateAdGroupRequest request) {
        
        AdGroupResponse response = adGroupService.updateAdGroup(id, campaignId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete ad group", description = "Soft delete an ad group")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Ad group deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Ad group not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteAdGroup(
            @Parameter(description = "Campaign ID", required = true)
            @PathVariable UUID campaignId,
            @Parameter(description = "Ad group ID", required = true)
            @PathVariable UUID id) {
        
        adGroupService.deleteAdGroup(id, campaignId);
        return ResponseEntity.noContent().build();
    }
}

