package com.travelo.admin.repository;

import com.travelo.admin.domain.AdminCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.util.StringUtils;

import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface AdminCategoryRepository
        extends JpaRepository<AdminCategory, Long>, JpaSpecificationExecutor<AdminCategory> {

    List<AdminCategory> findByActiveIsTrueOrderByNameAsc();

    static Specification<AdminCategory> withSearch(String q) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(q)) {
                return cb.conjunction();
            }
            String like = "%" + q.toLowerCase() + "%";
            return cb.like(cb.lower(root.get("name")), like);
        };
    }

    default Page<AdminCategory> findPage(String q, Pageable p) {
        if (!StringUtils.hasText(q)) {
            return findAll(p);
        }
        return findAll(withSearch(q.trim()), p);
    }
}
