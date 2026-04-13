package com.travelo.circlesservice.controller;

import com.travelo.circlesservice.dto.CirclesDiscoveryDto;
import com.travelo.circlesservice.service.CirclesDiscoveryService;
import com.travelo.postservice.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/circles")
public class CirclesDiscoveryController {

    private final CirclesDiscoveryService circlesDiscoveryService;

    public CirclesDiscoveryController(CirclesDiscoveryService circlesDiscoveryService) {
        this.circlesDiscoveryService = circlesDiscoveryService;
    }

    /**
     * Circles home: city, nearby count, and horizontal "People around you" list.
     */
    @GetMapping("/discovery")
    public ResponseEntity<ApiResponse<CirclesDiscoveryDto>> discovery(
            @RequestParam(value = "city", required = false) String city
    ) {
        CirclesDiscoveryDto body = circlesDiscoveryService.getDiscovery(city);
        return ResponseEntity.ok(ApiResponse.success("OK", body));
    }
}
