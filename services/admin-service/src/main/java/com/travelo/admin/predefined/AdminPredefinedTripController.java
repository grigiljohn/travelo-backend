package com.travelo.admin.predefined;

import com.travelo.admin.api.ApiResponse;
import com.travelo.admin.api.PageResponse;
import com.travelo.admin.domain.PredefinedTrip;
import com.travelo.admin.dto.PredefinedTripRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/predefined-trips")
public class AdminPredefinedTripController {
    private final PredefinedTripService service;

    public AdminPredefinedTripController(PredefinedTripService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PredefinedTrip>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(
                service.page(q, PageRequest.of(page, size, Sort.unsorted())))));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<ApiResponse<PredefinedTrip>> create(@Valid @RequestBody PredefinedTripRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Created", service.create(r)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<ApiResponse<PredefinedTrip>> update(
            @PathVariable long id, @Valid @RequestBody PredefinedTripRequest r) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, r)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
