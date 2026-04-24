package com.travelo.discoveryservice.curated;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public-facing REST endpoints for curated collections. These are the
 * "editor's picks" surface on the Explore tab — grouped, themed bundles
 * of notes/reels that the app curates centrally.
 *
 * <p>Two endpoints:
 * <ul>
 *   <li>{@code GET /api/v1/discovery/collections} — list rows for the grid</li>
 *   <li>{@code GET /api/v1/discovery/collections/{id}} — detail + paged notes</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/discovery/collections")
@Tag(name = "Curated Collections", description = "Editor-curated collections of notes")
public class CuratedCollectionsController {

    private static final Logger logger = LoggerFactory.getLogger(CuratedCollectionsController.class);

    private final CuratedCollectionsService curatedCollectionsService;

    public CuratedCollectionsController(CuratedCollectionsService curatedCollectionsService) {
        this.curatedCollectionsService = curatedCollectionsService;
    }

    @GetMapping
    @Operation(summary = "List curated collections",
               description = "Returns the configured set of curated collections with note counts.")
    public ResponseEntity<List<CuratedCollectionDto>> listCollections(
            @Parameter(description = "Viewer user id for privacy-filtered counts (optional)")
            @RequestHeader(value = "X-User-Id", required = false) String viewerId) {
        logger.debug("GET /api/v1/discovery/collections viewer={}", viewerId);
        return ResponseEntity.ok(curatedCollectionsService.listCollections(viewerId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get curated collection detail",
               description = "Returns the collection and a paged window of its matching notes.")
    public ResponseEntity<CuratedCollectionDetailDto> getCollection(
            @Parameter(description = "Collection id (as configured)") @PathVariable("id") String id,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") @Max(50) int limit,
            @RequestHeader(value = "X-User-Id", required = false) String viewerId) {
        logger.debug("GET /api/v1/discovery/collections/{} page={} limit={} viewer={}",
                id, page, limit, viewerId);
        return curatedCollectionsService.getCollectionDetail(id, page, limit, viewerId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
