package com.travelo.planservice.service;

import com.travelo.planservice.dto.PlanCreateRequest;
import com.travelo.planservice.dto.PlanResponse;
import com.travelo.planservice.persistence.CirclePlanEntity;
import com.travelo.planservice.persistence.CirclePlanParticipantEntity;
import com.travelo.planservice.repository.CirclePlanParticipantRepository;
import com.travelo.planservice.repository.CirclePlanRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PlanService {

    private static final String DEFAULT_HERO =
            "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=1200&q=80";

    private final CirclePlanRepository circlePlanRepository;
    private final CirclePlanParticipantRepository participantRepository;

    public PlanService(
            CirclePlanRepository circlePlanRepository,
            CirclePlanParticipantRepository participantRepository
    ) {
        this.circlePlanRepository = circlePlanRepository;
        this.participantRepository = participantRepository;
    }

    /**
     * @param city optional client locality for future geo-scoped filtering; currently ignored.
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unused")
    public List<PlanResponse> listPlans(String city) {
        List<CirclePlanEntity> plans = circlePlanRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        if (plans.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = plans.stream().map(CirclePlanEntity::getId).toList();
        Map<UUID, List<String>> avatars = loadAvatarPreviewByPlanIds(ids);
        return plans.stream().map(p -> toResponse(p, avatars.get(p.getId()))).toList();
    }

    /**
     * Recent published plans for home-feed blending (newest first, bounded).
     */
    @Transactional(readOnly = true)
    public List<PlanResponse> listRecentPlansForFeed(int limit) {
        int cap = Math.min(48, Math.max(1, limit));
        List<CirclePlanEntity> slice = circlePlanRepository.findTop48ByOrderByCreatedAtDesc().stream()
                .filter(p -> p.getStatus() == null || "PUBLISHED".equalsIgnoreCase(p.getStatus()))
                .limit(cap)
                .toList();
        if (slice.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = slice.stream().map(CirclePlanEntity::getId).toList();
        Map<UUID, List<String>> avatars = loadAvatarPreviewByPlanIds(ids);
        return slice.stream().map(p -> toResponse(p, avatars.get(p.getId()))).toList();
    }

    @Transactional
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
        UUID id = UUID.randomUUID();

        CirclePlanEntity row = new CirclePlanEntity();
        row.setId(id);
        row.setHostUserId(hostUserId);
        row.setOrganizerCommunityId(blankToNull(req.organizerCommunityId()));
        row.setTitle(req.title().trim());
        row.setDescription(desc);
        row.setLocationLabel(req.location().trim());
        row.setExternalPlaceId(blankToNull(req.externalPlaceId()));
        row.setLatitude(req.latitude());
        row.setLongitude(req.longitude());
        row.setStartsAt(parseStartsAt(req.startsAtIso()));
        row.setTimeLabel(req.timeLabel().trim());
        row.setMaxPeople(req.maxPeople());
        row.setJoinedCount(1);
        row.setBadge(badge);
        row.setHeroImageUrl(hero);
        row.setHostName(hostName);
        row.setHostAvatarUrl(hostAvatar);
        row.setPrivacy(normalizePrivacy(req.privacy()));
        row.setRequireApproval(Boolean.TRUE.equals(req.requireApprovalToJoin()));
        row.setAllowWaitlist(!Boolean.FALSE.equals(req.allowWaitlist()));
        row.setStatus("PUBLISHED");

        circlePlanRepository.save(row);

        CirclePlanParticipantEntity hostRow = new CirclePlanParticipantEntity();
        hostRow.setPlanId(id);
        hostRow.setUserId(hostUserId);
        hostRow.setDisplayName(hostName);
        hostRow.setAvatarUrl(hostAvatar);
        hostRow.setRole("HOST");
        participantRepository.save(hostRow);

        return toResponse(row, hostAvatar.isBlank() ? List.of() : List.of(hostAvatar));
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static OffsetDateTime parseStartsAt(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(iso.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static String normalizePrivacy(String raw) {
        if (raw == null || raw.isBlank()) {
            return "PUBLIC";
        }
        String u = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (u.contains("PRIVATE") || u.equals("PRIVATE_INVITE")) {
            return "PRIVATE_INVITE";
        }
        if (u.contains("FRIEND")) {
            return "FRIENDS_ONLY";
        }
        return "PUBLIC";
    }

    private Map<UUID, List<String>> loadAvatarPreviewByPlanIds(List<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<CirclePlanParticipantEntity> rows = participantRepository.findByPlanIdIn(ids);
        Map<UUID, List<CirclePlanParticipantEntity>> byPlan = rows.stream()
                .collect(Collectors.groupingBy(CirclePlanParticipantEntity::getPlanId, LinkedHashMap::new, Collectors.toList()));
        Map<UUID, List<String>> out = new LinkedHashMap<>();
        for (Map.Entry<UUID, List<CirclePlanParticipantEntity>> e : byPlan.entrySet()) {
            List<String> urls = e.getValue().stream()
                    .sorted(Comparator
                            .comparing((CirclePlanParticipantEntity p) -> "HOST".equalsIgnoreCase(p.getRole()) ? 0 : 1)
                            .thenComparing(CirclePlanParticipantEntity::getJoinedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(CirclePlanParticipantEntity::getAvatarUrl)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .limit(12)
                    .toList();
            out.put(e.getKey(), urls);
        }
        return out;
    }

    private PlanResponse toResponse(CirclePlanEntity e, List<String> previewAvatars) {
        List<String> avatars = previewAvatars == null || previewAvatars.isEmpty()
                ? new ArrayList<>()
                : new ArrayList<>(previewAvatars);
        if (avatars.isEmpty() && e.getHostAvatarUrl() != null && !e.getHostAvatarUrl().isBlank()) {
            avatars.add(e.getHostAvatarUrl().trim());
        }
        String org = e.getOrganizerCommunityId();
        return new PlanResponse(
                e.getId().toString(),
                e.getTitle(),
                e.getLocationLabel(),
                e.getTimeLabel(),
                e.getHostUserId(),
                e.getHostName(),
                e.getHostAvatarUrl() == null ? "" : e.getHostAvatarUrl(),
                List.copyOf(avatars),
                e.getJoinedCount(),
                e.getMaxPeople(),
                e.getBadge(),
                e.getHeroImageUrl() == null ? "" : e.getHeroImageUrl(),
                e.getDescription() == null ? "" : e.getDescription(),
                org == null || org.isBlank() ? "" : org
        );
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
