package com.travelo.admin.api.catalog;

import com.travelo.admin.api.ApiResponse;
import com.travelo.admin.predefined.PredefinedTripService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
public class PublicCatalogController {
    private final PublicCatalogService publicCatalogService;
    private final PredefinedTripService predefinedTripService;

    public PublicCatalogController(
            PublicCatalogService publicCatalogService,
            PredefinedTripService predefinedTripService) {
        this.publicCatalogService = publicCatalogService;
        this.predefinedTripService = predefinedTripService;
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<PublicCategoryDto>>> categories() {
        return ResponseEntity.ok(ApiResponse.ok(publicCatalogService.listActiveCategories()));
    }

    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<List<PublicTagDto>>> tags() {
        return ResponseEntity.ok(ApiResponse.ok(publicCatalogService.listActiveTags()));
    }

    @GetMapping("/predefined-trips")
    public ResponseEntity<ApiResponse<List<PublicPredefinedTripDto>>> predefinedTrips() {
        return ResponseEntity.ok(ApiResponse.ok(predefinedTripService.listActive()));
    }

    @GetMapping("/predefined-trips/{id}")
    public ResponseEntity<ApiResponse<PublicPredefinedTripDto>> predefinedTripById(@PathVariable long id) {
        return predefinedTripService
                .getActiveById(id)
                .map(d -> ResponseEntity.ok(ApiResponse.ok(d)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
