package com.travelo.admin.repository;

import com.travelo.admin.domain.AdminTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.util.StringUtils;

import java.util.List;

public interface AdminTagRepository
        extends JpaRepository<AdminTag, Long>, JpaSpecificationExecutor<AdminTag> {

    List<AdminTag> findByActiveIsTrueOrderByNameAsc();
    static Specification<AdminTag> withSearch(String q) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(q)) {
                return cb.conjunction();
            }
            String like = "%" + q.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("slug")), like)
            );
        };
    }

    default Page<AdminTag> findPage(String q, Pageable p) {
        return StringUtils.hasText(q) ? findAll(withSearch(q.trim()), p) : findAll(p);
    }

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long id);

    boolean existsBySlugIgnoreCase(String slug);
}
