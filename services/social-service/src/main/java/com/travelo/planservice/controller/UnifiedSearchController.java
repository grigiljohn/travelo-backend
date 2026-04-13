package com.travelo.planservice.controller;

import com.travelo.planservice.dto.UnifiedSearchResponse;
import com.travelo.planservice.service.UnifiedSearchService;
import com.travelo.postservice.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unified search for mobile: users, curated locations, trips, and live plans.
 */
@RestController
@RequestMapping("/api/v1/search")
public class UnifiedSearchController {

    private final UnifiedSearchService unifiedSearchService;

    public UnifiedSearchController(UnifiedSearchService unifiedSearchService) {
        this.unifiedSearchService = unifiedSearchService;
    }

    /**
     * @param q        query (min 2 chars recommended)
     * @param category optional: all | users | locations | trips | plans
     */
    @GetMapping("/unified")
    public ResponseEntity<ApiResponse<UnifiedSearchResponse>> unified(
            @RequestParam("q") String q,
            @RequestParam(value = "category", required = false, defaultValue = "all") String category
    ) {
        UnifiedSearchResponse body = unifiedSearchService.search(q, category);
        return ResponseEntity.ok(ApiResponse.success("OK", body));
    }
}
