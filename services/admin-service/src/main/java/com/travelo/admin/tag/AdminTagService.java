package com.travelo.admin.tag;

import com.travelo.admin.audit.AuditService;
import com.travelo.admin.domain.AdminTag;
import com.travelo.admin.dto.TagRequest;
import com.travelo.admin.repository.AdminTagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class AdminTagService {
    private final AdminTagRepository repository;
    private final AuditService audit;

    public AdminTagService(AdminTagRepository repository, AuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Page<AdminTag> page(String q, Pageable p) {
        return repository.findPage(q, p);
    }

    @Transactional
    public AdminTag create(TagRequest r) {
        var slug = r.slug().trim().toLowerCase().replace(" ", "-");
        if (repository.existsBySlugIgnoreCase(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "slug exists");
        }
        var t = new AdminTag();
        t.setName(r.name().trim());
        t.setSlug(slug);
        t.setActive(r.active());
        t = repository.save(t);
        audit.log("CREATE", "Tag", String.valueOf(t.getId()), null, toMap(t));
        return t;
    }

    @Transactional
    public AdminTag update(long id, TagRequest r) {
        var t = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var before = toMap(t);
        var newSlug = r.slug().trim().toLowerCase().replace(" ", "-");
        if (!newSlug.equalsIgnoreCase(t.getSlug()) && repository.existsBySlugIgnoreCaseAndIdNot(newSlug, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "slug exists");
        }
        t.setName(r.name().trim());
        t.setSlug(newSlug);
        t.setActive(r.active());
        t = repository.save(t);
        audit.log("UPDATE", "Tag", String.valueOf(t.getId()), before, toMap(t));
        return t;
    }

    @Transactional
    public void delete(long id) {
        var t = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        repository.delete(t);
        audit.log("DELETE", "Tag", String.valueOf(id), toMap(t), null);
    }

    private static Map<String, Object> toMap(AdminTag t) {
        return Map.of("name", t.getName(), "slug", t.getSlug(), "active", t.isActive());
    }
}
