package com.travelo.admin.moderation;

import com.travelo.admin.api.ApiResponse;
import com.travelo.admin.api.PageResponse;
import com.travelo.admin.domain.AdminReport;
import com.travelo.admin.dto.ResolveReportRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/reports")
public class ReportController {
    private final AdminReportService service;

    public ReportController(AdminReportService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminReport>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(
                service.page(q, PageRequest.of(page, size, Sort.by("createdAt").descending())))));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<ApiResponse<AdminReport>> resolve(@PathVariable long id, @Valid @RequestBody ResolveReportRequest r) {
        return ResponseEntity.ok(ApiResponse.ok(service.resolve(id, r)));
    }
}
