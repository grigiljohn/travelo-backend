package com.travelo.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocationRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 100) String country,
        Double latitude,
        Double longitude,
        boolean active
) {
}
