package com.travelo.planservice.service;

import com.travelo.planservice.dto.PlanCreateRequest;
import com.travelo.planservice.dto.PlanResponse;
import com.travelo.planservice.model.PlanEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class PlanService {

    private static final String DEFAULT_HERO =
            "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=1200&q=80";

    private final CopyOnWriteArrayList<PlanEntity> plans = new CopyOnWriteArrayList<>();

    /**
     * @param city optional client locality for future geo-scoped filtering; currently ignored.
     */
    @SuppressWarnings("unused")
    public List<PlanResponse> listPlans(String city) {
        return plans.stream()
                .sorted(Comparator.comparing(PlanEntity::getCreatedAt).reversed())
                .map(PlanEntity::toResponse)
                .toList();
    }

    public PlanResponse createPlan(
            String hostUserId,
            String headerHostName,
            PlanCreateRequest req
    ) {
        String badge = normalizeBadge(req.badge());
        String hero = req.heroImageUrl() != null && !req.heroImageUrl().isBlank()
                ? req.heroImageUrl().trim()
                : DEFAULT_HERO;
        String hostName = firstNonBlank(req.hostName(), headerHostName, "Traveler");
        String hostAvatar = req.hostAvatarUrl() != null ? req.hostAvatarUrl().trim() : "";
        String desc = req.description() != null ? req.description().trim() : "";
        if (desc.isEmpty()) {
            desc = "Hosted by " + hostName;
        }
        var entity = new PlanEntity(
                UUID.randomUUID().toString(),
                req.title().trim(),
                req.location().trim(),
                req.timeLabel().trim(),
                hostUserId,
                hostName,
                hostAvatar,
                List.of(),
                1,
                req.maxPeople(),
                badge,
                hero,
                desc,
                Instant.now()
        );
        plans.add(0, entity);
        return entity.toResponse();
    }

    private static String firstNonBlank(String a, String b, String fallback) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return fallback;
    }

    private static String normalizeBadge(String raw) {
        if (raw == null || raw.isBlank()) {
            return "NONE";
        }
        String u = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (u.equals("TRENDING")) {
            return "TRENDING";
        }
        if (u.equals("HAPPENING_NOW")) {
            return "HAPPENING_NOW";
        }
        return "NONE";
    }
}
