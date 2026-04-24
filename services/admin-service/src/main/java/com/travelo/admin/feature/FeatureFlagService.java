package com.travelo.admin.feature;

import com.travelo.admin.audit.AuditService;
import com.travelo.admin.domain.FeatureFlag;
import com.travelo.admin.domain.FeaturePlatform;
import com.travelo.admin.dto.FeatureFlagUpdateRequest;
import com.travelo.admin.repository.FeatureFlagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

@Service
public class FeatureFlagService {
    private final FeatureFlagRepository repository;
    private final AuditService audit;

    public FeatureFlagService(FeatureFlagRepository repository, AuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<FeatureFlag> all() {
        return repository.findAllByOrderByFeatureNameAsc();
    }

    @Transactional
    public FeatureFlag update(long id, FeatureFlagUpdateRequest req) {
        var f = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("flag not found"));
        var before = Map.of(
                "enabled", f.isEnabled(),
                "rollout", f.getRolloutPercentage(),
                "platform", f.getPlatform().name()
        );
        if (req.isEnabled() != null) f.setEnabled(req.isEnabled());
        if (req.rolloutPercentage() != null) {
            if (req.rolloutPercentage() < 0 || req.rolloutPercentage() > 100) {
                throw new IllegalArgumentException("rollout 0-100");
            }
            f.setRolloutPercentage(req.rolloutPercentage());
        }
        if (req.platform() != null) f.setPlatform(req.platform());
        f = repository.save(f);
        audit.log("UPDATE", "FeatureFlag", String.valueOf(f.getId()), before, Map.of(
                "enabled", f.isEnabled(),
                "rollout", f.getRolloutPercentage(),
                "platform", f.getPlatform().name()
        ));
        return f;
    }

    /**
     * Deterministic rollout: only applies when isEnabled; uses CRC32 of (featureName+userKey+platform).
     */
    public boolean evaluate(String featureName, FeaturePlatform platform, String userId) {
        var f = repository.findByFeatureNameIgnoreCaseAndPlatform(featureName, platform).orElse(null);
        if (f == null) {
            return false;
        }
        if (!f.isEnabled()) {
            return false;
        }
        if (f.getRolloutPercentage() >= 100) {
            return true;
        }
        if (f.getRolloutPercentage() <= 0) {
            return false;
        }
        var key = userId == null || userId.isBlank() ? "anon" : userId;
        var crc = new CRC32();
        var bytes = (featureName + "\0" + key + "\0" + platform.name()).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        crc.update(bytes);
        int bucket = (int) (crc.getValue() % 100L);
        return bucket < f.getRolloutPercentage();
    }
}
