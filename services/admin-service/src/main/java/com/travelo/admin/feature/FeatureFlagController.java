package com.travelo.admin.feature;

import com.travelo.admin.api.ApiResponse;
import com.travelo.admin.domain.FeatureFlag;
import com.travelo.admin.domain.FeaturePlatform;
import com.travelo.admin.dto.EvaluateFeatureRequest;
import com.travelo.admin.dto.FeatureFlagUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/feature-flags")
public class FeatureFlagController {
    private final FeatureFlagService service;

    public FeatureFlagController(FeatureFlagService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<FeatureFlag>>> all() {
        return ResponseEntity.ok(ApiResponse.ok(service.all()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<ApiResponse<FeatureFlag>> update(@PathVariable long id, @RequestBody @Valid FeatureFlagUpdateRequest r) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, r)));
    }

    @PostMapping("/evaluate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> evaluate(@Valid @RequestBody EvaluateFeatureRequest r) {
        boolean on = service.evaluate(r.featureName(), r.platform(), r.userId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("enabled", on, "featureName", r.featureName(), "platform", r.platform().name())));
    }
}
