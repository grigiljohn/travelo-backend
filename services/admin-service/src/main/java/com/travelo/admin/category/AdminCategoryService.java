package com.travelo.admin.category;

import com.travelo.admin.audit.AuditService;
import com.travelo.admin.domain.AdminCategory;
import com.travelo.admin.dto.CategoryRequest;
import com.travelo.admin.repository.AdminCategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class AdminCategoryService {
    private final AdminCategoryRepository repository;
    private final AuditService audit;

    public AdminCategoryService(AdminCategoryRepository repository, AuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Page<AdminCategory> page(String q, Pageable p) {
        return repository.findPage(q, p);
    }

    @Transactional
    public AdminCategory create(CategoryRequest req) {
        var c = new AdminCategory();
        c.setName(req.name().trim());
        c.setIcon(req.icon() == null ? "" : req.icon().trim());
        c.setActive(req.active());
        c = repository.save(c);
        audit.log("CREATE", "Category", String.valueOf(c.getId()), null, toMap(c));
        return c;
    }

    @Transactional
    public AdminCategory update(long id, CategoryRequest req) {
        var c = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var before = toMap(c);
        c.setName(req.name().trim());
        c.setIcon(req.icon() == null ? "" : req.icon().trim());
        c.setActive(req.active());
        c = repository.save(c);
        audit.log("UPDATE", "Category", String.valueOf(c.getId()), before, toMap(c));
        return c;
    }

    @Transactional
    public void delete(long id) {
        var c = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        repository.delete(c);
        audit.log("DELETE", "Category", String.valueOf(id), toMap(c), null);
    }

    private static Map<String, Object> toMap(AdminCategory c) {
        return Map.of(
                "name", c.getName(),
                "icon", c.getIcon(),
                "active", c.isActive()
        );
    }
}
