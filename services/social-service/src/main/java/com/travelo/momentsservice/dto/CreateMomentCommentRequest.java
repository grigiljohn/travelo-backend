package com.travelo.momentsservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMomentCommentRequest(
        @NotBlank @Size(max = 2000) String commentText
) {
}
