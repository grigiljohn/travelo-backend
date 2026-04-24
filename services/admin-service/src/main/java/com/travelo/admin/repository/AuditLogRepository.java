package com.travelo.admin.repository;

import com.travelo.admin.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    static Specification<AuditLog> filters(String actor, String action, String from, String to) {
        return (root, query, cb) -> {
            var p = cb.conjunction();
            if (StringUtils.hasText(actor)) {
                p = cb.and(p, cb.like(cb.lower(root.get("actorId")), "%" + actor.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(action)) {
                p = cb.and(p, cb.like(cb.lower(root.get("action")), "%" + action.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(from)) {
                p = cb.and(p, cb.greaterThanOrEqualTo(root.get("createdAt"), OffsetDateTime.parse(from)));
            }
            if (StringUtils.hasText(to)) {
                p = cb.and(p, cb.lessThanOrEqualTo(root.get("createdAt"), OffsetDateTime.parse(to)));
            }
            return p;
        };
    }

    default Page<AuditLog> findPage(String actor, String action, String from, String to, Pageable p) {
        if (!StringUtils.hasText(actor) && !StringUtils.hasText(action) && !StringUtils.hasText(from) && !StringUtils.hasText(to)) {
            return findAll(p);
        }
        return findAll(filters(actor, action, from, to), p);
    }
}
