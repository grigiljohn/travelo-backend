package com.travelo.admin.repository;

import com.travelo.admin.domain.AdminReport;
import com.travelo.admin.domain.AdminReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.util.StringUtils;

public interface AdminReportRepository
        extends JpaRepository<AdminReport, Long>, JpaSpecificationExecutor<AdminReport> {

    long countByStatus(AdminReportStatus status);

    static Specification<AdminReport> withSearch(String q) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(q)) {
                return cb.conjunction();
            }
            String like = "%" + q.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("reason")), like),
                    cb.like(cb.lower(root.get("targetId")), like)
            );
        };
    }

    default Page<AdminReport> findPage(String q, Pageable p) {
        return StringUtils.hasText(q) ? findAll(withSearch(q.trim()), p) : findAll(p);
    }
}
