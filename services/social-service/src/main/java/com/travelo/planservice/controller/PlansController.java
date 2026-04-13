package com.travelo.planservice.controller;

import com.travelo.commons.security.SecurityUtils;
import com.travelo.planservice.dto.PlanCreateRequest;
import com.travelo.planservice.dto.PlanResponse;
import com.travelo.planservice.dto.RichPlanCreateRequest;
import com.travelo.planservice.dto.RichPlanDetailResponse;
import com.travelo.planservice.dto.RichPlanResponse;
import com.travelo.planservice.service.PlanService;
import com.travelo.planservice.service.RichPlanService;
import com.travelo.postservice.dto.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
public class PlansController {

    private static final Logger logger = LoggerFactory.getLogger(PlansController.class);
    private final PlanService planService;
    private final RichPlanService richPlanService;

    public PlansController(PlanService planService, RichPlanService richPlanService) {
        this.planService = planService;
        this.richPlanService = richPlanService;
    }

    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<List<RichPlanResponse>>> feedRichPlans() {
        return ResponseEntity.ok(ApiResponse.success("OK", richPlanService.feed()));
    }

    @GetMapping("/{planId:[a-fA-F0-9\\-]{36}}")
    public ResponseEntity<ApiResponse<RichPlanDetailResponse>> richPlanById(
            @PathVariable String planId,
            @RequestHeader(value = "X-User-Id", required = false) String viewerUserId
    ) {
        RichPlanDetailResponse d = richPlanService.getDetail(planId, viewerUserId);
        if (d == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Plan not found", "PLAN_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponse.success("OK", d));
    }

    /**
     * Join a rich plan (requires JWT). Idempotent if user already joined.
     */
    @PostMapping("/{planId:[a-fA-F0-9\\-]{36}}/join")
    public ResponseEntity<ApiResponse<RichPlanDetailResponse>> joinRichPlan(
            @PathVariable String planId,
            @RequestHeader(value = "X-User-Name", required = false) String headerUserName,
            @RequestHeader(value = "X-User-Avatar", required = false) String headerUserAvatar
    ) {
        String userId = SecurityUtils.getCurrentUserIdAsString();
        if (userId == null) {
            throw new com.travelo.postservice.exception.UnauthorizedException("User not authenticated");
        }
        try {
            RichPlanDetailResponse d = richPlanService.join(planId, userId, headerUserName, headerUserAvatar);
            return ResponseEntity.ok(ApiResponse.success("Joined", d));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error("Plan is full", "PLAN_FULL"));
            }
            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlanResponse>>> listPlans(
            @RequestParam(value = "city", required = false) String city
    ) {
        List<PlanResponse> list = planService.listPlans(city);
        return ResponseEntity.ok(ApiResponse.success("OK", list));
    }

    /**
     * Next-gen create (rich model + feed ranking fields).
     */
    @PostMapping(value = "/create", consumes = "application/json")
    public ResponseEntity<ApiResponse<RichPlanResponse>> createRichPlan(
            @Valid @RequestBody RichPlanCreateRequest request,
            @RequestHeader(value = "X-User-Name", required = false) String headerUserName
    ) {
        String userId = SecurityUtils.getCurrentUserIdAsString();
        if (userId == null) {
            logger.warn("Unauthenticated rich plan create attempt");
            throw new com.travelo.postservice.exception.UnauthorizedException("User not authenticated");
        }
        RichPlanResponse created = richPlanService.create(userId, headerUserName, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Plan created", created));
    }

    /**
     * Create a travel plan (Circles). Requires a valid JWT (same as posts).
     */
    @PostMapping(consumes = "application/json")
    public ResponseEntity<ApiResponse<PlanResponse>> createPlan(
            @Valid @RequestBody PlanCreateRequest request,
            @RequestHeader(value = "X-User-Name", required = false) String headerUserName
    ) {
        String userId = SecurityUtils.getCurrentUserIdAsString();
        if (userId == null) {
            logger.warn("Unauthenticated plan create attempt");
            throw new com.travelo.postservice.exception.UnauthorizedException("User not authenticated");
        }
        PlanResponse created = planService.createPlan(userId, headerUserName, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Plan created", created));
    }
}
