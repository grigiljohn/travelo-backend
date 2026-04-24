package com.travelo.admin.dto;

import com.travelo.admin.domain.AdminReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveReportRequest(
        @NotNull AdminReportStatus status,
        @Size(max = 2000) String note
) {
}
