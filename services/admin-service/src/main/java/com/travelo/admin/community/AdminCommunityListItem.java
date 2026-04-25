package com.travelo.admin.community;

import java.time.OffsetDateTime;

public record AdminCommunityListItem(
        Long id,
        String externalId,
        String name,
        String description,
        String tagline,
        String city,
        String coverImageUrl,
        String iconImageUrl,
        String tagsCsv,
        String topicsCsv,
        String rulesText,
        int memberCount,
        int onlineCount,
        boolean requireAdminApproval,
        boolean allowMemberInvites,
        String visibility,
        boolean active,
        OffsetDateTime createdAt,
        String source
) {
}
