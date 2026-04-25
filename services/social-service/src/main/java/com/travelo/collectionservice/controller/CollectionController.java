package com.travelo.collectionservice.controller;

import com.travelo.collectionservice.dto.*;
import com.travelo.collectionservice.entity.CollectionType;
import com.travelo.collectionservice.service.CollectionService;
import com.travelo.commons.security.SecurityUtils;
import com.travelo.postservice.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/collections")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CollectionSummaryDto>> create(@Valid @RequestBody CreateCollectionRequest request) {
        String userId = requiredUserId();
        CollectionSummaryDto created = collectionService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Collection created", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CollectionSummaryDto>>> list(
            @RequestParam(value = "type", required = false) CollectionType type,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        String userId = requiredUserId();
        Page<CollectionSummaryDto> result = collectionService.list(userId, type, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("Collections fetched", result));
    }

    @GetMapping("/{collectionId}")
    public ResponseEntity<ApiResponse<CollectionDetailDto>> detail(@PathVariable UUID collectionId) {
        String userId = requiredUserId();
        CollectionDetailDto detail = collectionService.getDetail(userId, collectionId);
        return ResponseEntity.ok(ApiResponse.success("Collection detail", detail));
    }

    @PostMapping("/{collectionId}/media")
    public ResponseEntity<ApiResponse<Void>> addMedia(
            @PathVariable UUID collectionId,
            @Valid @RequestBody AddCollectionMediaRequest request
    ) {
        String userId = requiredUserId();
        collectionService.addMedia(userId, collectionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Media added"));
    }

    @GetMapping("/{collectionId}/media")
    public ResponseEntity<ApiResponse<Page<CollectionMediaDto>>> listMedia(
            @PathVariable UUID collectionId,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "30") @Min(1) @Max(120) int size
    ) {
        String userId = requiredUserId();
        Page<CollectionMediaDto> result = collectionService.listMedia(userId, collectionId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("Collection media fetched", result));
    }

    @PostMapping("/auto/trips/{tripId}")
    public ResponseEntity<ApiResponse<CollectionSummaryDto>> ensureAutoTripCollection(
            @PathVariable String tripId,
            @RequestParam(value = "tripTitle", required = false) String tripTitle,
            @RequestParam(value = "coverImageUrl", required = false) String coverImageUrl
    ) {
        String userId = requiredUserId();
        CollectionSummaryDto dto = collectionService.ensureAutoForTrip(userId, tripId, tripTitle, coverImageUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Auto collection ready", dto));
    }

    private String requiredUserId() {
        String userId = SecurityUtils.getCurrentUserIdAsString();
        if (userId == null || userId.isBlank()) {
            throw new SecurityException("User not authenticated");
        }
        return userId;
    }
}
