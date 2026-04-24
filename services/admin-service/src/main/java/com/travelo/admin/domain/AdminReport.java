package com.travelo.admin.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "admin_reports")
@Getter
@Setter
@NoArgsConstructor
public class AdminReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "reporter_id", nullable = false, length = 64)
    private String reporterId;
    @Column(name = "target_type", nullable = false, length = 32)
    private String targetType;
    @Column(name = "target_id", nullable = false, length = 128)
    private String targetId;
    @Column(nullable = false, length = 500)
    private String reason;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String details = "";
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminReportStatus status = AdminReportStatus.PENDING;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;
    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;
}
