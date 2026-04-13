package com.travelo.circlesservice.controller;

import com.travelo.circlesservice.dto.CommunityResponse;
import com.travelo.circlesservice.dto.CreateCommunityRequest;
import com.travelo.circlesservice.service.CircleCommunityService;
import com.travelo.postservice.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/circles/communities")
public class CircleCommunityController {

    private final CircleCommunityService communityService;

    public CircleCommunityController(CircleCommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommunityResponse>>> list(
            @RequestParam(value = "city", required = false) String city,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success("OK", communityService.list(city, userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CommunityResponse>> getOne(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success("OK", communityService.get(id, userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CommunityResponse>> create(
            @RequestBody CreateCommunityRequest body,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Created", communityService.create(body, userId)));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<CommunityResponse>> join(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Joined", communityService.join(id, userId)));
    }
}
