package com.travelo.circlesservice.service;

import com.travelo.circlesservice.dto.CommunityResponse;
import com.travelo.circlesservice.dto.CreateCommunityRequest;
import com.travelo.circlesservice.dto.PendingJoinsResponse;
import com.travelo.circlesservice.dto.UpdateCommunityRequest;
import com.travelo.circlesservice.persistence.CircleCommunityEntity;
import com.travelo.circlesservice.persistence.CircleCommunityMemberEntity;
import com.travelo.circlesservice.persistence.CircleCommunityTagEntity;
import com.travelo.circlesservice.persistence.MemberPk;
import com.travelo.circlesservice.persistence.MembershipStatus;
import com.travelo.circlesservice.repository.CircleCommunityMemberRepository;
import com.travelo.circlesservice.repository.CircleCommunityRepository;
import com.travelo.circlesservice.repository.CircleCommunityTagRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CircleCommunityService {

    private static final int MAX_URL_LEN = 2048;
    private static final int MAX_RULES_LEN = 8000;
    private static final int MAX_DESC_LEN = 4000;
    private static final int MAX_TAGLINE_LEN = 500;

    private final CircleCommunityRepository communityRepository;
    private final CircleCommunityMemberRepository memberRepository;
    private final CircleCommunityTagRepository tagRepository;

    public CircleCommunityService(
            CircleCommunityRepository communityRepository,
            CircleCommunityMemberRepository memberRepository,
            CircleCommunityTagRepository tagRepository
    ) {
        this.communityRepository = communityRepository;
        this.memberRepository = memberRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<CommunityResponse> list(String city, String currentUserId) {
        String user = blankToGuest(currentUserId);
        List<CircleCommunityEntity> accessible = communityRepository.findAccessibleForUser(user);
        if (accessible.isEmpty()) {
            return List.of();
        }
        List<String> ids = accessible.stream().map(CircleCommunityEntity::getId).toList();
        Set<String> memberOf = new HashSet<>(memberRepository.findCommunityIdsContainingUser(user, ids));
        Map<String, List<String>> tagsByCommunity = loadTagsByCommunityIds(ids);

        List<CommunityResponse> all = accessible.stream()
                .sorted(Comparator
                        .comparing((CircleCommunityEntity c) -> !memberOf.contains(c.getId()))
                        .thenComparing(CircleCommunityEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .map(c -> toDto(c, tagsByCommunity.getOrDefault(c.getId(), List.of()), user))
                .collect(Collectors.toList());

        if (city == null || city.isBlank()) {
            return all;
        }
        String q = city.trim();
        if ("nearby".equalsIgnoreCase(q)) {
            return all;
        }
        List<CommunityResponse> narrowed = all.stream()
                .filter(d -> matchesCityQuery(q, d.getCity()))
                .collect(Collectors.toList());
        if (!narrowed.isEmpty()) {
            return narrowed;
        }
        return all;
    }

    @Transactional
    public CommunityResponse create(CreateCommunityRequest req, String ownerId) {
        String user = blankToGuest(ownerId);
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name required");
        }
        String id = "c_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        LinkedHashSet<String> memberUsers = new LinkedHashSet<>();
        memberUsers.add(user);
        if (req.getInviteUserIds() != null) {
            for (String invitee : req.getInviteUserIds()) {
                if (invitee == null) {
                    continue;
                }
                String x = invitee.trim();
                if (x.isEmpty() || x.equals(user)) {
                    continue;
                }
                memberUsers.add(x);
            }
        }

        CircleCommunityEntity c = new CircleCommunityEntity();
        c.setId(id);
        c.setName(req.getName().trim());
        c.setDescription(clamp(req.getDescription() == null ? "" : req.getDescription().trim(), MAX_DESC_LEN));
        c.setTagline(clamp(req.getTagline() == null ? "" : req.getTagline().trim(), MAX_TAGLINE_LEN));
        c.setCity(req.getCity() == null ? "" : req.getCity().trim());
        c.setVisibility(normalizeVis(req.getVisibility()));
        c.setOwnerUserId(user);
        c.setMemberCount(memberUsers.size());
        c.setLastActivityLabel("Just now");
        c.setRulesText(clamp(req.getRules() == null ? "" : req.getRules().trim(), MAX_RULES_LEN));
        c.setTopics(normalizeTopicList(req.getTopics()));
        c.setCoverImageUrl(clampUrl(req.getCoverImageUrl()));
        c.setIconImageUrl(clampUrl(req.getIconImageUrl()));
        if (Boolean.TRUE.equals(req.getRequireAdminApproval())) {
            c.setRequireAdminApproval(true);
        }
        if (Boolean.FALSE.equals(req.getAllowMemberInvites())) {
            c.setAllowMemberInvites(false);
        } else {
            c.setAllowMemberInvites(true);
        }
        communityRepository.save(c);

        for (String uid : memberUsers) {
            CircleCommunityMemberEntity row = new CircleCommunityMemberEntity();
            row.setCommunityId(id);
            row.setUserId(uid);
            row.setMembershipStatus(MembershipStatus.ACTIVE);
            row.setRole(uid.equals(user) ? "OWNER" : "MEMBER");
            memberRepository.save(row);
        }
        if (req.getTags() != null) {
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            for (String tag : req.getTags()) {
                if (tag == null) {
                    continue;
                }
                String t = tag.trim();
                if (t.isEmpty() || t.length() > 48 || !seen.add(t)) {
                    continue;
                }
                CircleCommunityTagEntity te = new CircleCommunityTagEntity();
                te.setCommunityId(id);
                te.setTag(t);
                tagRepository.save(te);
            }
        }
        return get(id, ownerId);
    }

    @Transactional
    public CommunityResponse join(String id, String userId) {
        String user = blankToGuest(userId);
        CircleCommunityEntity c = communityRepository.findById(id).orElseThrow(() -> notFound());
        if (!visibleTo(c, user)) {
            throw notFound();
        }
        if (memberRepository.existsByCommunityIdAndUserIdAndMembershipStatus(id, user, MembershipStatus.ACTIVE)) {
            return get(id, userId);
        }
        if (memberRepository.existsByCommunityIdAndUserIdAndMembershipStatus(id, user, MembershipStatus.PENDING)) {
            return get(id, userId);
        }
        String vis = visKey(c.getVisibility());
        if (!"public".equals(vis)) {
            throw notFound();
        }
        CircleCommunityMemberEntity row = new CircleCommunityMemberEntity();
        row.setCommunityId(id);
        row.setUserId(user);
        row.setRole("MEMBER");
        if (c.isRequireAdminApproval()) {
            row.setMembershipStatus(MembershipStatus.PENDING);
            memberRepository.save(row);
        } else {
            row.setMembershipStatus(MembershipStatus.ACTIVE);
            memberRepository.save(row);
            communityRepository.incrementMemberCount(id, "Just now");
        }
        return get(id, userId);
    }

    @Transactional
    public CommunityResponse cancelPendingJoin(String id, String userId) {
        String user = blankToGuest(userId);
        CircleCommunityEntity c = communityRepository.findById(id).orElseThrow(() -> notFound());
        if (!visibleTo(c, user)) {
            throw notFound();
        }
        if (!memberRepository.existsByCommunityIdAndUserIdAndMembershipStatus(id, user, MembershipStatus.PENDING)) {
            return get(id, userId);
        }
        memberRepository.deleteById(new MemberPk(id, user));
        return get(id, userId);
    }

    @Transactional(readOnly = true)
    public PendingJoinsResponse listPendingJoins(String id, String ownerUserId) {
        String owner = blankToGuest(ownerUserId);
        CircleCommunityEntity c = communityRepository.findById(id).orElseThrow(() -> notFound());
        assertOwner(c, owner);
        return new PendingJoinsResponse(memberRepository.findPendingUserIds(id));
    }

    @Transactional
    public CommunityResponse approveJoinRequest(String id, String targetUserId, String ownerUserId) {
        String owner = blankToGuest(ownerUserId);
        CircleCommunityEntity c = communityRepository.findById(id).orElseThrow(() -> notFound());
        assertOwner(c, owner);
        String target = targetUserId == null ? "" : targetUserId.trim();
        if (target.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId required");
        }
        CircleCommunityMemberEntity row = memberRepository.findById(new MemberPk(id, target)).orElseThrow(() -> notFound());
        if (row.getMembershipStatus() != MembershipStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "no pending request for user");
        }
        row.setMembershipStatus(MembershipStatus.ACTIVE);
        memberRepository.save(row);
        communityRepository.incrementMemberCount(id, "Just now");
        return get(id, ownerUserId);
    }

    @Transactional
    public CommunityResponse rejectJoinRequest(String id, String targetUserId, String ownerUserId) {
        String owner = blankToGuest(ownerUserId);
        CircleCommunityEntity c = communityRepository.findById(id).orElseThrow(() -> notFound());
        assertOwner(c, owner);
        String target = targetUserId == null ? "" : targetUserId.trim();
        if (target.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId required");
        }
        CircleCommunityMemberEntity row = memberRepository.findById(new MemberPk(id, target)).orElseThrow(() -> notFound());
        if (row.getMembershipStatus() != MembershipStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "no pending request for user");
        }
        memberRepository.delete(row);
        return get(id, ownerUserId);
    }

    @Transactional
    public CommunityResponse update(String id, UpdateCommunityRequest req, String userId) {
        String user = blankToGuest(userId);
        CircleCommunityEntity c = communityRepository.findById(id).orElseThrow(() -> notFound());
        assertOwner(c, user);

        if (req.getName() != null) {
            String n = req.getName().trim();
            if (n.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name cannot be empty");
            }
            c.setName(n.length() > 200 ? n.substring(0, 200) : n);
        }
        if (req.getDescription() != null) {
            c.setDescription(clamp(req.getDescription().trim(), MAX_DESC_LEN));
        }
        if (req.getTagline() != null) {
            c.setTagline(clamp(req.getTagline().trim(), MAX_TAGLINE_LEN));
        }
        if (req.getCity() != null) {
            c.setCity(req.getCity().trim());
        }
        if (req.getVisibility() != null) {
            c.setVisibility(normalizeVis(req.getVisibility()));
        }
        if (req.getRules() != null) {
            c.setRulesText(clamp(req.getRules().trim(), MAX_RULES_LEN));
        }
        if (req.getTopics() != null) {
            c.setTopics(normalizeTopicList(req.getTopics()));
        }
        if (req.getCoverImageUrl() != null) {
            String u = req.getCoverImageUrl().trim();
            c.setCoverImageUrl(u.isEmpty() ? null : clampUrl(u));
        }
        if (req.getIconImageUrl() != null) {
            String u = req.getIconImageUrl().trim();
            c.setIconImageUrl(u.isEmpty() ? null : clampUrl(u));
        }
        if (req.getRequireAdminApproval() != null) {
            c.setRequireAdminApproval(req.getRequireAdminApproval());
        }
        if (req.getAllowMemberInvites() != null) {
            c.setAllowMemberInvites(req.getAllowMemberInvites());
        }
        if (req.getTags() != null) {
            tagRepository.deleteByCommunityId(id);
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            for (String tag : req.getTags()) {
                if (tag == null) {
                    continue;
                }
                String t = tag.trim();
                if (t.isEmpty() || t.length() > 48 || !seen.add(t)) {
                    continue;
                }
                CircleCommunityTagEntity te = new CircleCommunityTagEntity();
                te.setCommunityId(id);
                te.setTag(t);
                tagRepository.save(te);
            }
        }
        communityRepository.save(c);
        return get(id, userId);
    }

    @Transactional(readOnly = true)
    public CommunityResponse get(String id, String userId) {
        String user = blankToGuest(userId);
        CircleCommunityEntity c = communityRepository.findById(id).orElseThrow(() -> notFound());
        if (!visibleTo(c, user)) {
            throw notFound();
        }
        List<String> tags = tagRepository.findByCommunityIdInOrderByTagAsc(List.of(id)).stream()
                .map(CircleCommunityTagEntity::getTag)
                .collect(Collectors.toList());
        return toDto(c, tags, user);
    }

    private Map<String, List<String>> loadTagsByCommunityIds(List<String> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<CircleCommunityTagEntity> rows = tagRepository.findByCommunityIdInOrderByTagAsc(ids);
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (CircleCommunityTagEntity t : rows) {
            map.computeIfAbsent(t.getCommunityId(), k -> new ArrayList<>()).add(t.getTag());
        }
        return map;
    }

    private CommunityResponse toDto(CircleCommunityEntity c, List<String> tags, String user) {
        boolean activeMember = memberRepository.existsByCommunityIdAndUserIdAndMembershipStatus(
                c.getId(), user, MembershipStatus.ACTIVE);
        boolean pending = memberRepository.existsByCommunityIdAndUserIdAndMembershipStatus(
                c.getId(), user, MembershipStatus.PENDING);
        boolean owner = c.getOwnerUserId().equals(user);
        CommunityResponse r = new CommunityResponse();
        r.setId(c.getId());
        r.setName(c.getName());
        r.setDescription(c.getDescription());
        r.setTagline(c.getTagline() != null ? c.getTagline() : "");
        r.setTags(tags);
        r.setTopics(c.getTopics() != null ? List.copyOf(c.getTopics()) : List.of());
        r.setRules(c.getRulesText() != null ? c.getRulesText() : "");
        r.setCoverImageUrl(c.getCoverImageUrl());
        r.setIconImageUrl(c.getIconImageUrl());
        r.setVisibility(c.getVisibility());
        r.setCity(c.getCity());
        r.setMemberCount(c.getMemberCount());
        r.setLastActivity(c.getLastActivityLabel());
        r.setMember(activeMember);
        r.setOwner(owner);
        r.setPendingJoinRequest(pending && !activeMember);
        r.setRequireAdminApproval(c.isRequireAdminApproval());
        r.setAllowMemberInvites(c.isAllowMemberInvites());
        return r;
    }

    private boolean visibleTo(CircleCommunityEntity c, String user) {
        String vis = visKey(c.getVisibility());
        if ("public".equals(vis)) {
            return true;
        }
        if (c.getOwnerUserId().equals(user)) {
            return true;
        }
        return memberRepository.existsByCommunityIdAndUserIdAndMembershipStatus(c.getId(), user, MembershipStatus.ACTIVE);
    }

    private void assertOwner(CircleCommunityEntity c, String user) {
        if (!c.getOwnerUserId().equals(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "owner only");
        }
    }

    private static List<String> normalizeTopicList(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String s : raw) {
            if (s == null) {
                continue;
            }
            String t = s.trim();
            if (t.isEmpty() || t.length() > 80) {
                continue;
            }
            seen.add(t);
        }
        return new ArrayList<>(seen);
    }

    private static String clamp(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    private static String clampUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String u = url.trim();
        if (u.length() > MAX_URL_LEN) {
            return u.substring(0, MAX_URL_LEN);
        }
        return u;
    }

    private static boolean matchesCityQuery(String requested, String communityCity) {
        if (communityCity == null || communityCity.isBlank()) {
            return true;
        }
        String a = requested.trim().toLowerCase(Locale.ROOT);
        String b = communityCity.trim().toLowerCase(Locale.ROOT);
        return a.contains(b) || b.contains(a);
    }

    private static String blankToGuest(String id) {
        if (id == null || id.isBlank()) {
            return "guest";
        }
        return id.trim();
    }

    private static String normalizeVis(String v) {
        if (v == null) {
            return "public";
        }
        String x = v.trim().toLowerCase(Locale.ROOT);
        if ("secret".equals(x)) {
            return "secret";
        }
        if ("private".equals(x)) {
            return "private";
        }
        return "public";
    }

    private static String visKey(String v) {
        if (v == null) {
            return "public";
        }
        return v.trim().toLowerCase(Locale.ROOT);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "community not found");
    }
}
