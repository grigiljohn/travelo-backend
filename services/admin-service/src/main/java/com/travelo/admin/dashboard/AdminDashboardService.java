package com.travelo.admin.dashboard;

import com.travelo.admin.community.AdminManagedCommunityService;
import com.travelo.admin.domain.AdminReportStatus;
import com.travelo.admin.repository.AdminReportRepository;
import com.travelo.admin.repository.AdminUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
public class AdminDashboardService {
    @Value("${app.metrics.users-total-override:}")
    private String usersOverride;
    @Value("${app.metrics.posts-total-override:}")
    private String postsOverride;
    private final AdminUserRepository adminUsers;
    private final AdminReportRepository reports;
    private final AdminManagedCommunityService communities;

    public AdminDashboardService(
            AdminUserRepository adminUsers,
            AdminReportRepository reports,
            AdminManagedCommunityService communities) {
        this.adminUsers = adminUsers;
        this.reports = reports;
        this.communities = communities;
    }

    public Map<String, Object> stats() {
        long userTotal = StringUtils.hasText(usersOverride) ? Long.parseLong(usersOverride) : 0L;
        long postTotal = StringUtils.hasText(postsOverride) ? Long.parseLong(postsOverride) : 0L;
        return Map.of(
                "usersTotal", userTotal,
                "postsTotal", postTotal,
                "adminUsers", adminUsers.count(),
                "pendingReports", reports.countByStatus(AdminReportStatus.PENDING),
                "activeCommunities", communities.countActive()
        );
    }
}
