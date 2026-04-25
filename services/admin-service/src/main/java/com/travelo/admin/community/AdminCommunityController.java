package com.travelo.admin.community;

import com.travelo.admin.api.ApiResponse;
import com.travelo.admin.api.PageResponse;
import com.travelo.admin.domain.AdminManagedCommunity;
import com.travelo.admin.dto.CommunityRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/communities")
public class AdminCommunityController {
    private final AdminManagedCommunityService service;

    public AdminCommunityController(AdminManagedCommunityService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminCommunityListItem>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.page(q, PageRequest.of(page, size, Sort.by("id").descending()))));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<ApiResponse<AdminManagedCommunity>> create(@Valid @RequestBody CommunityRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Created", service.create(r)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<ApiResponse<AdminManagedCommunity>> update(@PathVariable long id, @Valid @RequestBody CommunityRequest r) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, r)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
