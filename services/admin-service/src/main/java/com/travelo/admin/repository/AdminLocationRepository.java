package com.travelo.admin.repository;

import com.travelo.admin.domain.AdminLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.util.StringUtils;

public interface AdminLocationRepository
        extends JpaRepository<AdminLocation, Long>, JpaSpecificationExecutor<AdminLocation> {
    static Specification<AdminLocation> withSearch(String q) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(q)) {
                return cb.conjunction();
            }
            String like = "%" + q.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("country")), like)
            );
        };
    }

    default Page<AdminLocation> findPage(String q, Pageable p) {
        return StringUtils.hasText(q) ? findAll(withSearch(q.trim()), p) : findAll(p);
    }
}
