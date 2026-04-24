package com.travelo.admin.api.catalog;

import com.travelo.admin.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
public class PublicCatalogController {
    private final PublicCatalogService publicCatalogService;

    public PublicCatalogController(PublicCatalogService publicCatalogService) {
        this.publicCatalogService = publicCatalogService;
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<PublicCategoryDto>>> categories() {
        return ResponseEntity.ok(ApiResponse.ok(publicCatalogService.listActiveCategories()));
    }

    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<List<PublicTagDto>>> tags() {
        return ResponseEntity.ok(ApiResponse.ok(publicCatalogService.listActiveTags()));
    }
}
