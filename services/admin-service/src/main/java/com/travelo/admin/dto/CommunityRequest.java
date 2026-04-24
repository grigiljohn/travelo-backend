package com.travelo.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunityRequest(
        @Size(max = 64) String externalId,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 4000) String description,
        @Size(max = 200) String city,
        @Size(max = 16) String visibility,
        boolean active
) {
}
