package com.travelo.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunityRequest(
        @Size(max = 64) String externalId,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 4000) String description,
        @Size(max = 500) String tagline,
        @Size(max = 200) String city,
        @Size(max = 2048) String coverImageUrl,
        @Size(max = 2048) String iconImageUrl,
        @Size(max = 4000) String tagsCsv,
        @Size(max = 4000) String topicsCsv,
        @Size(max = 8000) String rulesText,
        int memberCount,
        int onlineCount,
        Boolean requireAdminApproval,
        Boolean allowMemberInvites,
        @Size(max = 16) String visibility,
        boolean active
) {
}
