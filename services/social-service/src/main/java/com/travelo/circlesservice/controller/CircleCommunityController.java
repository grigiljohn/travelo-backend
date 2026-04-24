package com.travelo.circlesservice.controller;

import com.travelo.circlesservice.dto.CommunityResponse;
import com.travelo.circlesservice.dto.CreateCommunityRequest;
import com.travelo.circlesservice.dto.PendingJoinsResponse;
import com.travelo.circlesservice.dto.UpdateCommunityRequest;
import com.travelo.circlesservice.service.CircleCommunityService;
import com.travelo.postservice.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        CommunityResponse created = communityService.create(body, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Created", created));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CommunityResponse>> patch(
            @PathVariable("id") String id,
            @RequestBody UpdateCommunityRequest body,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Updated", communityService.update(id, body, userId)));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<CommunityResponse>> join(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Joined", communityService.join(id, userId)));
    }

    @DeleteMapping("/{id}/pending-join")
    public ResponseEntity<ApiResponse<CommunityResponse>> cancelPendingJoin(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Cancelled", communityService.cancelPendingJoin(id, userId)));
    }

    @GetMapping("/{id}/join-requests")
    public ResponseEntity<ApiResponse<PendingJoinsResponse>> listJoinRequests(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success("OK", communityService.listPendingJoins(id, userId)));
    }

    @PostMapping("/{id}/join-requests/{userId}/approve")
    public ResponseEntity<ApiResponse<CommunityResponse>> approveJoin(
            @PathVariable("id") String id,
            @PathVariable("userId") String targetUserId,
            @RequestHeader(value = "X-User-Id", required = false) String ownerUserId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Approved", communityService.approveJoinRequest(id, targetUserId, ownerUserId)));
    }

    @PostMapping("/{id}/join-requests/{userId}/reject")
    public ResponseEntity<ApiResponse<CommunityResponse>> rejectJoin(
            @PathVariable("id") String id,
            @PathVariable("userId") String targetUserId,
            @RequestHeader(value = "X-User-Id", required = false) String ownerUserId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Rejected", communityService.rejectJoinRequest(id, targetUserId, ownerUserId)));
    }
}
