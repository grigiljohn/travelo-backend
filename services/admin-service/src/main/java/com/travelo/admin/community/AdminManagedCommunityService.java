package com.travelo.admin.community;

import com.travelo.admin.audit.AuditService;
import com.travelo.admin.domain.AdminManagedCommunity;
import com.travelo.admin.dto.CommunityRequest;
import com.travelo.admin.repository.AdminManagedCommunityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class AdminManagedCommunityService {
    private final AdminManagedCommunityRepository repository;
    private final AuditService audit;

    public AdminManagedCommunityService(AdminManagedCommunityRepository repository, AuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Page<AdminManagedCommunity> page(String q, Pageable p) {
        return repository.findPage(q, p);
    }

    @Transactional
    public AdminManagedCommunity create(CommunityRequest r) {
        var c = new AdminManagedCommunity();
        c.setExternalId(r.externalId() == null ? null : r.externalId().trim());
        c.setName(r.name().trim());
        c.setDescription(r.description() == null ? "" : r.description().trim());
        c.setCity(r.city() == null ? "" : r.city().trim());
        c.setVisibility(StringUtils.hasText(r.visibility()) ? r.visibility().trim() : "public");
        c.setActive(r.active());
        c = repository.save(c);
        audit.log("CREATE", "AdminCommunity", String.valueOf(c.getId()), null, toMap(c));
        return c;
    }

    @Transactional
    public AdminManagedCommunity update(long id, CommunityRequest r) {
        var c = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var b = toMap(c);
        c.setExternalId(r.externalId() == null ? null : r.externalId().trim());
        c.setName(r.name().trim());
        c.setDescription(r.description() == null ? "" : r.description().trim());
        c.setCity(r.city() == null ? "" : r.city().trim());
        c.setVisibility(StringUtils.hasText(r.visibility()) ? r.visibility().trim() : "public");
        c.setActive(r.active());
        c = repository.save(c);
        audit.log("UPDATE", "AdminCommunity", String.valueOf(c.getId()), b, toMap(c));
        return c;
    }

    @Transactional
    public void delete(long id) {
        var c = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        repository.delete(c);
        audit.log("DELETE", "AdminCommunity", String.valueOf(id), toMap(c), null);
    }

    @Transactional(readOnly = true)
    public long countActive() {
        return repository.countByActiveTrue();
    }

    private static Map<String, Object> toMap(AdminManagedCommunity c) {
        return Map.of("name", c.getName(), "active", c.isActive(), "visibility", c.getVisibility());
    }
}
