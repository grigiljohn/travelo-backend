package com.travelo.planservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiGeneratePlanRequest(
        @NotBlank @Size(max = 500) String prompt
) {
}
