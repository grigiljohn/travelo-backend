package com.travelo.planservice.controller;

import com.travelo.planservice.dto.AiGeneratePlanRequest;
import com.travelo.planservice.dto.AiGeneratePlanResponse;
import com.travelo.planservice.service.AiPlanService;
import com.travelo.postservice.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiPlanController {

    private final AiPlanService aiPlanService;

    public AiPlanController(AiPlanService aiPlanService) {
        this.aiPlanService = aiPlanService;
    }

    @PostMapping(value = "/generate-plan", consumes = "application/json")
    public ResponseEntity<ApiResponse<AiGeneratePlanResponse>> generatePlan(
            @Valid @RequestBody AiGeneratePlanRequest request
    ) {
        AiGeneratePlanResponse body = aiPlanService.generate(request.prompt());
        return ResponseEntity.ok(ApiResponse.success("OK", body));
    }

    @PostMapping(value = "/enhance-plan-description", consumes = "application/json")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> enhance(
            @RequestBody java.util.Map<String, String> body
    ) {
        String draft = body.getOrDefault("text", "");
        String improved = aiPlanService.enhanceDescription(draft);
        return ResponseEntity.ok(ApiResponse.success("OK", java.util.Map.of("description", improved)));
    }
}
