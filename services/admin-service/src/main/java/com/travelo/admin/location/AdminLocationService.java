package com.travelo.admin.location;

import com.travelo.admin.audit.AuditService;
import com.travelo.admin.domain.AdminLocation;
import com.travelo.admin.dto.LocationRequest;
import com.travelo.admin.repository.AdminLocationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class AdminLocationService {
    private final AdminLocationRepository repository;
    private final AuditService audit;

    public AdminLocationService(AdminLocationRepository repository, AuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Page<AdminLocation> page(String q, Pageable p) {
        return repository.findPage(q, p);
    }

    @Transactional
    public AdminLocation create(LocationRequest r) {
        var e = new AdminLocation();
        e.setName(r.name().trim());
        e.setCountry(r.country() == null ? "" : r.country().trim());
        e.setLatitude(r.latitude());
        e.setLongitude(r.longitude());
        e.setActive(r.active());
        e = repository.save(e);
        audit.log("CREATE", "Location", String.valueOf(e.getId()), null, toMap(e));
        return e;
    }

    @Transactional
    public AdminLocation update(long id, LocationRequest r) {
        var e = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var b = toMap(e);
        e.setName(r.name().trim());
        e.setCountry(r.country() == null ? "" : r.country().trim());
        e.setLatitude(r.latitude());
        e.setLongitude(r.longitude());
        e.setActive(r.active());
        e = repository.save(e);
        audit.log("UPDATE", "Location", String.valueOf(e.getId()), b, toMap(e));
        return e;
    }

    @Transactional
    public void delete(long id) {
        var e = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        repository.delete(e);
        audit.log("DELETE", "Location", String.valueOf(id), toMap(e), null);
    }

    private static Map<String, Object> toMap(AdminLocation e) {
        return Map.of(
                "name", e.getName(),
                "country", e.getCountry(),
                "active", e.isActive()
        );
    }
}
