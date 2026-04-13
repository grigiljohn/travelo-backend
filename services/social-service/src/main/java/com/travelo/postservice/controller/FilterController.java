package com.travelo.postservice.controller;

import com.travelo.postservice.dto.ApiResponse;
import com.travelo.postservice.dto.FilterDto;
import com.travelo.postservice.service.FilterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/filters")
public class FilterController {
    private static final Logger logger = LoggerFactory.getLogger(FilterController.class);
    private final FilterService filterService;

    public FilterController(FilterService filterService) {
        this.filterService = filterService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FilterDto>>> getFilters(
            @RequestParam(required = false) String type) {
        try {
            List<FilterDto> filters = type != null 
                ? filterService.getFiltersByType(type)
                : filterService.getAllFilters();
            return ResponseEntity.ok(ApiResponse.success("Filters fetched successfully", filters));
        } catch (Exception e) {
            logger.error("Error fetching filters", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch filters: " + e.getMessage(), "FILTER_FETCH_FAILED"));
        }
    }
}

