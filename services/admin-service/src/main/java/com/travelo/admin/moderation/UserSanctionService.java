package com.travelo.admin.moderation;

import com.travelo.admin.audit.AuditService;
import com.travelo.admin.domain.ModerationAction;
import com.travelo.admin.domain.UserSanction;
import com.travelo.admin.repository.ModerationActionRepository;
import com.travelo.admin.repository.UserSanctionRepository;
import com.travelo.admin.support.CurrentAdminId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class UserSanctionService {
    private final UserSanctionRepository repository;
    private final ModerationActionRepository actions;
    private final AuditService audit;

    public UserSanctionService(
            UserSanctionRepository repository,
            ModerationActionRepository actions,
            AuditService audit
    ) {
        this.repository = repository;
        this.actions = actions;
        this.audit = audit;
    }

    @Transactional
    public UserSanction banUser(String userId) {
        var s = repository.findById(userId).orElseGet(() -> {
            var x = new UserSanction();
            x.setUserId(userId);
            return x;
        });
        s.setBanned(true);
        s.setRestricted(false);
        s.setBannedAt(OffsetDateTime.now());
        s.setUpdatedAt(OffsetDateTime.now());
        s = repository.save(s);
        var a = new ModerationAction();
        a.setActorId(CurrentAdminId.fromContext());
        a.setActionType("BAN_USER");
        a.setNote("banned " + userId);
        actions.save(a);
        audit.log("BAN", "User", userId, null, Map.of("banned", true));
        return s;
    }

    @Transactional
    public UserSanction restrictUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        var s = repository.findById(userId).orElseGet(() -> {
            var x = new UserSanction();
            x.setUserId(userId);
            return x;
        });
        s.setRestricted(true);
        s.setRestrictedAt(OffsetDateTime.now());
        s.setUpdatedAt(OffsetDateTime.now());
        s = repository.save(s);
        var a = new ModerationAction();
        a.setActorId(CurrentAdminId.fromContext());
        a.setActionType("RESTRICT_USER");
        a.setNote("restricted " + userId);
        actions.save(a);
        audit.log("RESTRICT", "User", userId, null, Map.of("restricted", true));
        return s;
    }
}
