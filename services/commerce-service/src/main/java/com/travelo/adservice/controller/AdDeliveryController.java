package com.travelo.adservice.controller;

import com.travelo.adservice.dto.AdDeliveryRequest;
import com.travelo.adservice.dto.AdDeliveryResponse;
import com.travelo.adservice.service.AdDeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for ad delivery to feed-service and reel-service.
 * Provides APIs to fetch ads for specific placements with targeting.
 */
@RestController
@RequestMapping("/api/v1/ads/delivery")
@Tag(name = "Ad Delivery", description = "Ad delivery APIs for feed and reel services")
public class AdDeliveryController {

    private static final Logger log = LoggerFactory.getLogger(AdDeliveryController.class);

    private final AdDeliveryService adDeliveryService;

    public AdDeliveryController(AdDeliveryService adDeliveryService) {
        this.adDeliveryService = adDeliveryService;
    }

    @PostMapping("/fetch")
    @Operation(summary = "Fetch ads for placement", 
               description = "Fetch ads for a specific placement (feed or reel) with targeting")
    public ResponseEntity<List<AdDeliveryResponse>> fetchAds(
            @Valid @RequestBody AdDeliveryRequest request,
            @Parameter(description = "Number of ads to fetch", example = "5")
            @RequestParam(value = "count", defaultValue = "5") int count) {
        
        log.info("Fetching {} ads for placement: {}, userId: {}", count, request.placement(), request.userId());
        
        List<AdDeliveryResponse> ads = adDeliveryService.fetchAdsForPlacement(request, count);
        
        return ResponseEntity.ok(ads);
    }
}

