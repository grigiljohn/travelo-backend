package com.travelo.admin.moderation;

import com.travelo.admin.audit.AuditService;
import com.travelo.admin.domain.*;
import com.travelo.admin.dto.ResolveReportRequest;
import com.travelo.admin.repository.AdminReportRepository;
import com.travelo.admin.repository.ModerationActionRepository;
import com.travelo.admin.support.CurrentAdminId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class AdminReportService {
    private final AdminReportRepository reports;
    private final ModerationActionRepository actions;
    private final AuditService audit;

    public AdminReportService(AdminReportRepository reports, ModerationActionRepository actions, AuditService audit) {
        this.reports = reports;
        this.actions = actions;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Page<AdminReport> page(String q, Pageable p) {
        return reports.findPage(q, p);
    }

    @Transactional
    public AdminReport resolve(long id, ResolveReportRequest req) {
        var r = reports.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var before = Map.of("status", r.getStatus().name());
        r.setStatus(req.status() != null ? req.status() : AdminReportStatus.RESOLVED);
        r.setResolvedAt(OffsetDateTime.now());
        r.setResolutionNote(req.note() == null ? "" : req.note().trim());
        r = reports.save(r);
        var a = new ModerationAction();
        a.setReportId(r.getId());
        a.setActorId(CurrentAdminId.fromContext());
        a.setActionType("RESOLVE");
        a.setNote(req.note() == null ? "" : req.note().trim());
        actions.save(a);
        audit.log("REPORT_RESOLVE", "Report", String.valueOf(r.getId()), before, Map.of("status", r.getStatus().name()));
        return r;
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return reports.countByStatus(AdminReportStatus.PENDING);
    }
}
