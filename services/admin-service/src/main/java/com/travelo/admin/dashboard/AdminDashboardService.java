package com.travelo.admin.dashboard;

import com.travelo.admin.community.AdminManagedCommunityService;
import com.travelo.admin.domain.AdminReportStatus;
import com.travelo.admin.repository.AdminReportRepository;
import com.travelo.admin.repository.AdminUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
public class AdminDashboardService {
    private static final String SOURCE_LIVE = "live";
    private static final String SOURCE_FALLBACK = "fallback";

    @Value("${app.metrics.users-total-override:}")
    private String usersOverride;
    @Value("${app.metrics.posts-total-override:}")
    private String postsOverride;
    @Value("${app.identity.base-url:http://localhost:8081}")
    private String identityBaseUrl;
    @Value("${app.social.base-url:http://localhost:8096}")
    private String socialBaseUrl;

    private final AdminUserRepository adminUsers;
    private final AdminReportRepository reports;
    private final AdminManagedCommunityService communities;
    private final RestTemplate mediaRestTemplate;
    private final ObjectMapper objectMapper;

    public AdminDashboardService(
            AdminUserRepository adminUsers,
            AdminReportRepository reports,
            AdminManagedCommunityService communities,
            RestTemplate mediaRestTemplate,
            ObjectMapper objectMapper) {
        this.adminUsers = adminUsers;
        this.reports = reports;
        this.communities = communities;
        this.mediaRestTemplate = mediaRestTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> stats() {
        MetricResult userTotal = fetchUsersTotal();
        MetricResult postTotal = fetchPostsTotal();
        return Map.of(
                "usersTotal", userTotal.value(),
                "usersTotalSource", userTotal.source(),
                "postsTotal", postTotal.value(),
                "postsTotalSource", postTotal.source(),
                "adminUsers", adminUsers.count(),
                "pendingReports", reports.countByStatus(AdminReportStatus.PENDING),
                "activeCommunities", communities.countActive()
        );
    }

    private MetricResult fetchUsersTotal() {
        try {
            String raw = mediaRestTemplate.getForObject(
                    identityBaseUrl.replaceAll("/$", "") + "/api/v1/users/metrics/counts", String.class);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode n = root.path("usersTotal");
            if (!n.isMissingNode() && !n.isNull()) return new MetricResult(n.asLong(), SOURCE_LIVE);
            JsonNode data = root.path("data");
            if (!data.isMissingNode()) return new MetricResult(data.path("usersTotal").asLong(0L), SOURCE_LIVE);
        } catch (Exception ignored) {
        }
        return new MetricResult(
                StringUtils.hasText(usersOverride) ? Long.parseLong(usersOverride) : 0L,
                SOURCE_FALLBACK
        );
    }

    private MetricResult fetchPostsTotal() {
        try {
            String raw = mediaRestTemplate.getForObject(
                    socialBaseUrl.replaceAll("/$", "") + "/api/v1/posts/metrics/counts", String.class);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode n = root.path("postsTotal");
            if (!n.isMissingNode() && !n.isNull()) return new MetricResult(n.asLong(), SOURCE_LIVE);
            JsonNode data = root.path("data");
            if (!data.isMissingNode()) return new MetricResult(data.path("postsTotal").asLong(0L), SOURCE_LIVE);
        } catch (Exception ignored) {
        }
        return new MetricResult(
                StringUtils.hasText(postsOverride) ? Long.parseLong(postsOverride) : 0L,
                SOURCE_FALLBACK
        );
    }

    private record MetricResult(long value, String source) {}
}
