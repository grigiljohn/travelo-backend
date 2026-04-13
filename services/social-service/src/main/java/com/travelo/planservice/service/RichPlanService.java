package com.travelo.planservice.service;

import com.travelo.planservice.dto.PlanChatPreviewMessage;
import com.travelo.planservice.dto.PlanEngagementView;
import com.travelo.planservice.dto.PlanHostView;
import com.travelo.planservice.dto.PlanParticipantPreview;
import com.travelo.planservice.dto.PlanStepInput;
import com.travelo.planservice.dto.PlanStepResponse;
import com.travelo.planservice.dto.RichPlanCreateRequest;
import com.travelo.planservice.dto.RichPlanDetailResponse;
import com.travelo.planservice.dto.RichPlanResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RichPlanService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final Map<String, RichPlanResponse> byId = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<String> order = new CopyOnWriteArrayList<>();
    private final Map<String, PlanSocialState> socialByPlanId = new ConcurrentHashMap<>();

    public RichPlanResponse create(
            String userId,
            String headerHostName,
            RichPlanCreateRequest req
    ) {
        String id = UUID.randomUUID().toString();
        Instant created = Instant.now();
        String host = firstNonBlank(req.hostName(), headerHostName, "Traveler");
        String timeLabel = humanTimeLabel(req.dateTimeIso());
        List<PlanStepResponse> steps = mapSteps(id, req.steps());
        double engagement = computeEngagementScore(req);
        String hostAvatar = req.hostAvatarUrl() == null ? "" : req.hostAvatarUrl().trim();
        RichPlanResponse resp = new RichPlanResponse(
                id,
                userId,
                req.title().trim(),
                req.description() == null ? "" : req.description().trim(),
                req.locationName().trim(),
                req.lat(),
                req.lng(),
                req.dateTimeIso().trim(),
                timeLabel,
                req.maxPeople(),
                req.audience().trim(),
                req.skillLevel().trim(),
                List.copyOf(req.tags()),
                req.mediaUrl().trim(),
                req.mediaType().trim().toLowerCase(Locale.ROOT),
                normalizeBadge(req.badge()),
                req.visibility().trim().toUpperCase(Locale.ROOT),
                req.paid(),
                req.pricePerPerson(),
                steps,
                host,
                hostAvatar,
                engagement,
                0,
                0,
                ISO.format(created.atOffset(ZoneOffset.UTC))
        );
        byId.put(id, resp);
        order.add(0, id);
        initSocial(id, userId, host, hostAvatar);
        return resp;
    }

    private void initSocial(String planId, String hostUserId, String hostName, String hostAvatar) {
        PlanSocialState st = new PlanSocialState();
        st.userIds.add(hostUserId);
        if (hostAvatar != null && !hostAvatar.isBlank()) {
            st.orderedAvatars.add(hostAvatar);
        }
        st.chatPreview.add(new PlanChatPreviewMessage(hostName, "Welcome to the plan chat 👋"));
        st.chatPreview.add(new PlanChatPreviewMessage("Travelo", "Ask the host anything here."));
        socialByPlanId.put(planId, st);
    }

    public RichPlanResponse getById(String id) {
        RichPlanResponse r = byId.get(id);
        if (r == null) {
            return null;
        }
        return bumpViews(r);
    }

    public RichPlanDetailResponse getDetail(String id, String viewerUserId) {
        RichPlanResponse r = getById(id);
        if (r == null) {
            return null;
        }
        return toDetail(r, viewerUserId);
    }

    public RichPlanDetailResponse join(String planId, String userId, String userName, String userAvatar) {
        RichPlanResponse r = byId.get(planId);
        if (r == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found");
        }
        PlanSocialState st = socialOrCreate(planId, r);
        if (st.userIds.contains(userId)) {
            return toDetail(r, userId);
        }
        if (st.userIds.size() >= r.maxPeople()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PLAN_FULL");
        }
        st.userIds.add(userId);
        String avatar = (userAvatar == null || userAvatar.isBlank())
                ? defaultAvatarForUser(userId)
                : userAvatar.trim();
        st.orderedAvatars.add(avatar);
        String label = (userName == null || userName.isBlank()) ? "Traveler" : userName.trim();
        st.chatPreview.add(new PlanChatPreviewMessage(label, "Just joined the plan 🎉"));
        trimChat(st);
        return toDetail(r, userId);
    }

    private static void trimChat(PlanSocialState st) {
        while (st.chatPreview.size() > 8) {
            st.chatPreview.remove(0);
        }
    }

    private PlanSocialState socialOrCreate(String planId, RichPlanResponse r) {
        return socialByPlanId.computeIfAbsent(planId, pid -> {
            PlanSocialState st = new PlanSocialState();
            String hostId = r.userId() == null ? "host" : r.userId();
            st.userIds.add(hostId);
            String av = r.hostAvatarUrl() == null ? "" : r.hostAvatarUrl().trim();
            if (!av.isBlank()) {
                st.orderedAvatars.add(av);
            }
            st.chatPreview.add(new PlanChatPreviewMessage(r.hostName(), "Welcome to the plan chat 👋"));
            return st;
        });
    }

    private RichPlanDetailResponse toDetail(RichPlanResponse r, String viewerUserId) {
        PlanSocialState st = socialOrCreate(r.id(), r);
        PlanHostView host = new PlanHostView(
                r.userId() == null ? "" : r.userId(),
                r.hostName(),
                r.hostAvatarUrl() == null ? "" : r.hostAvatarUrl(),
                4.8,
                r.hostAvatarUrl() != null && !r.hostAvatarUrl().isBlank()
        );
        List<PlanParticipantPreview> participants = st.orderedAvatars.stream()
                .map(PlanParticipantPreview::new)
                .toList();
        int participantsCount = st.userIds.size();
        List<PlanChatPreviewMessage> preview = lastChatMessages(st, 3);
        int viewsLastHour = viewsLastHourEstimate(r);
        PlanEngagementView engagement = new PlanEngagementView(viewsLastHour, r.viewsCount());
        BigDecimal price = r.paid() && r.pricePerPerson() != null ? r.pricePerPerson() : BigDecimal.ZERO;
        boolean viewerJoined = viewerUserId != null && !viewerUserId.isBlank() && st.userIds.contains(viewerUserId);
        return new RichPlanDetailResponse(
                r.id(),
                r.title(),
                r.description(),
                r.mediaUrl(),
                r.mediaType(),
                host,
                r.dateTimeIso(),
                r.locationName(),
                r.lat(),
                r.lng(),
                price,
                r.paid(),
                r.maxPeople(),
                participantsCount,
                participants,
                preview,
                engagement,
                r.timeLabel(),
                r.steps(),
                r.badge(),
                r.audience(),
                r.skillLevel(),
                r.tags(),
                viewerJoined
        );
    }

    private static List<PlanChatPreviewMessage> lastChatMessages(PlanSocialState st, int n) {
        List<PlanChatPreviewMessage> all = st.chatPreview;
        if (all.size() <= n) {
            return List.copyOf(all);
        }
        return List.copyOf(all.subList(all.size() - n, all.size()));
    }

    private static int viewsLastHourEstimate(RichPlanResponse r) {
        int v = r.viewsCount();
        int h = Math.abs(r.id().hashCode() % 17);
        return Math.min(99, Math.max(3, v / 2 + h));
    }

    private static String defaultAvatarForUser(String userId) {
        int seed = Math.abs(userId.hashCode() % 1000);
        return "https://picsum.photos/seed/travelo-" + seed + "/200/200";
    }

    public List<RichPlanResponse> feed() {
        return order.stream()
                .map(byId::get)
                .filter(p -> p != null)
                .sorted(Comparator.comparingDouble(RichPlanResponse::engagementScore).reversed()
                        .thenComparing(RichPlanResponse::createdAtIso, Comparator.reverseOrder()))
                .toList();
    }

    private RichPlanResponse bumpViews(RichPlanResponse r) {
        RichPlanResponse next = new RichPlanResponse(
                r.id(), r.userId(), r.title(), r.description(), r.locationName(), r.lat(), r.lng(),
                r.dateTimeIso(), r.timeLabel(), r.maxPeople(), r.audience(), r.skillLevel(), r.tags(),
                r.mediaUrl(), r.mediaType(), r.badge(), r.visibility(), r.paid(), r.pricePerPerson(),
                r.steps(), r.hostName(), r.hostAvatarUrl(), r.engagementScore(), r.viewsCount() + 1,
                r.likesCount(), r.createdAtIso()
        );
        byId.put(r.id(), next);
        return next;
    }

    private static List<PlanStepResponse> mapSteps(String planId, List<PlanStepInput> inputs) {
        return inputs.stream().map(s -> new PlanStepResponse(
                planId + "_s_" + s.stepOrder(),
                s.stepOrder(),
                s.title() == null ? "" : s.title().trim(),
                s.time() == null ? "" : s.time().trim()
        )).toList();
    }

    private static double computeEngagementScore(RichPlanCreateRequest req) {
        double score = 40;
        if (req.tags() != null) {
            score += Math.min(20, req.tags().size() * 4.0);
        }
        if (req.description() != null && req.description().length() > 80) {
            score += 10;
        }
        if ("TRENDING".equalsIgnoreCase(req.badge())) {
            score += 25;
        } else if ("HAPPENING_NOW".equalsIgnoreCase(req.badge())) {
            score += 15;
        }
        if (req.steps() != null && !req.steps().isEmpty()) {
            score += 10;
        }
        return score;
    }

    private static String humanTimeLabel(String iso) {
        try {
            OffsetDateTime odt = OffsetDateTime.parse(iso);
            return odt.toLocalDate() + " · " + odt.toLocalTime().withSecond(0).withNano(0);
        } catch (Exception e) {
            return iso;
        }
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

    private static String firstNonBlank(String a, String b, String fallback) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return fallback;
    }

    private static final class PlanSocialState {
        private final Set<String> userIds = ConcurrentHashMap.newKeySet();
        private final List<String> orderedAvatars = new CopyOnWriteArrayList<>();
        private final List<PlanChatPreviewMessage> chatPreview = new CopyOnWriteArrayList<>();
    }
}
