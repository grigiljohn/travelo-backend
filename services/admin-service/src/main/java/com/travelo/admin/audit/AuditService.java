package com.travelo.admin.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelo.admin.domain.AuditLog;
import com.travelo.admin.repository.AuditLogRepository;
import com.travelo.admin.support.CurrentAdminId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuditService {
    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public void log(String action, String entityType, String entityId, Object before, Object after) {
        var log = new AuditLog();
        log.setActorId(CurrentAdminId.fromContext());
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        if (before != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> b = (Map<String, Object>) objectMapper.convertValue(before, Map.class);
            log.setBeforeData(b);
        }
        if (after != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> a = (Map<String, Object>) objectMapper.convertValue(after, Map.class);
            log.setAfterData(a);
        }
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> list(String actor, String action, String from, String to, Pageable p) {
        return repository.findPage(actor, action, from, to, p);
    }
}
