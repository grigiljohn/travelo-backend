package com.travelo.admin.tag;

import com.travelo.admin.api.ApiResponse;
import com.travelo.admin.api.PageResponse;
import com.travelo.admin.domain.AdminTag;
import com.travelo.admin.dto.TagRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/tags")
public class TagController {
    private final AdminTagService service;

    public TagController(AdminTagService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminTag>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(
                service.page(q, PageRequest.of(page, size, Sort.by("id").descending())))));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<ApiResponse<AdminTag>> create(@Valid @RequestBody TagRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Created", service.create(r)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<ApiResponse<AdminTag>> update(@PathVariable long id, @Valid @RequestBody TagRequest r) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, r)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
