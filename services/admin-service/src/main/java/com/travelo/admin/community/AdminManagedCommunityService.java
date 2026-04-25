package com.travelo.admin.community;

import com.travelo.admin.audit.AuditService;
import com.travelo.admin.domain.AdminManagedCommunity;
import com.travelo.admin.dto.CommunityRequest;
import com.travelo.admin.repository.AdminManagedCommunityRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Service
public class AdminManagedCommunityService {
    private final AdminManagedCommunityRepository repository;
    private final AuditService audit;
    private final CircleCommunityClient circleCommunityClient;

    public AdminManagedCommunityService(
            AdminManagedCommunityRepository repository,
            AuditService audit,
            CircleCommunityClient circleCommunityClient) {
        this.repository = repository;
        this.audit = audit;
        this.circleCommunityClient = circleCommunityClient;
    }

    @Transactional(readOnly = true)
    public com.travelo.admin.api.PageResponse<AdminCommunityListItem> page(String q, Pageable p) {
        String query = q == null ? "" : q.trim();
        List<AdminCommunityListItem> merged = new ArrayList<>();

        List<AdminManagedCommunity> adminRows = org.springframework.util.StringUtils.hasText(query)
                ? repository.findAll(AdminManagedCommunityRepository.withSearch(query))
                : repository.findAll();
        adminRows.stream()
                .sorted(Comparator.comparing(AdminManagedCommunity::getId).reversed())
                .forEach(c -> merged.add(new AdminCommunityListItem(
                        c.getId(),
                        c.getExternalId(),
                        c.getName(),
                        c.getDescription(),
                        c.getTagline(),
                        c.getCity(),
                        c.getCoverImageUrl(),
                        c.getIconImageUrl(),
                        c.getTagsCsv(),
                        c.getTopicsCsv(),
                        c.getRulesText(),
                        c.getMemberCount(),
                        c.getOnlineCount(),
                        c.isRequireAdminApproval(),
                        c.isAllowMemberInvites(),
                        c.getVisibility(),
                        c.isActive(),
                        c.getCreatedAt(),
                        "admin"
                )));

        List<AdminCommunityListItem> circles = circleCommunityClient.fetchCommunities(query);
        Set<String> seenExternal = new HashSet<>();
        for (AdminCommunityListItem row : merged) {
            if (row.externalId() != null && !row.externalId().isBlank()) {
                seenExternal.add(row.externalId().trim().toLowerCase());
            }
        }
        for (AdminCommunityListItem row : circles) {
            String ext = row.externalId() == null ? "" : row.externalId().trim().toLowerCase();
            if (!ext.isEmpty() && seenExternal.contains(ext)) continue;
            merged.add(row);
        }

        int page = Math.max(p.getPageNumber(), 0);
        int size = Math.max(p.getPageSize(), 1);
        int from = page * size;
        int to = Math.min(from + size, merged.size());
        List<AdminCommunityListItem> content = from >= merged.size() ? List.of() : merged.subList(from, to);
        int totalPages = merged.isEmpty() ? 1 : (int) Math.ceil((double) merged.size() / size);

        return new com.travelo.admin.api.PageResponse<>(content, page, size, merged.size(), totalPages);
    }

    @Transactional
    public AdminManagedCommunity create(CommunityRequest r) {
        var c = new AdminManagedCommunity();
        c.setExternalId(r.externalId() == null ? null : r.externalId().trim());
        c.setName(r.name().trim());
        c.setDescription(r.description() == null ? "" : r.description().trim());
        c.setTagline(r.tagline() == null ? "" : r.tagline().trim());
        c.setCity(r.city() == null ? "" : r.city().trim());
        c.setCoverImageUrl(r.coverImageUrl() == null ? "" : r.coverImageUrl().trim());
        c.setIconImageUrl(r.iconImageUrl() == null ? "" : r.iconImageUrl().trim());
        c.setTagsCsv(r.tagsCsv() == null ? "" : r.tagsCsv().trim());
        c.setTopicsCsv(r.topicsCsv() == null ? "" : r.topicsCsv().trim());
        c.setRulesText(r.rulesText() == null ? "" : r.rulesText().trim());
        c.setMemberCount(Math.max(0, r.memberCount()));
        c.setOnlineCount(Math.max(0, r.onlineCount()));
        c.setRequireAdminApproval(Boolean.TRUE.equals(r.requireAdminApproval()));
        c.setAllowMemberInvites(!Boolean.FALSE.equals(r.allowMemberInvites()));
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
        c.setTagline(r.tagline() == null ? "" : r.tagline().trim());
        c.setCity(r.city() == null ? "" : r.city().trim());
        c.setCoverImageUrl(r.coverImageUrl() == null ? "" : r.coverImageUrl().trim());
        c.setIconImageUrl(r.iconImageUrl() == null ? "" : r.iconImageUrl().trim());
        c.setTagsCsv(r.tagsCsv() == null ? "" : r.tagsCsv().trim());
        c.setTopicsCsv(r.topicsCsv() == null ? "" : r.topicsCsv().trim());
        c.setRulesText(r.rulesText() == null ? "" : r.rulesText().trim());
        c.setMemberCount(Math.max(0, r.memberCount()));
        c.setOnlineCount(Math.max(0, r.onlineCount()));
        c.setRequireAdminApproval(Boolean.TRUE.equals(r.requireAdminApproval()));
        c.setAllowMemberInvites(!Boolean.FALSE.equals(r.allowMemberInvites()));
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
        return Map.of(
                "name", c.getName(),
                "active", c.isActive(),
                "visibility", c.getVisibility(),
                "city", c.getCity(),
                "memberCount", c.getMemberCount(),
                "onlineCount", c.getOnlineCount()
        );
    }
}
