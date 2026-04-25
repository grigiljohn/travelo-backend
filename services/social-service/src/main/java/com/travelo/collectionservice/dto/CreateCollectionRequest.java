package com.travelo.collectionservice.dto;

import com.travelo.collectionservice.entity.CollectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCollectionRequest(
        @NotBlank @Size(max = 140) String title,
        @NotNull CollectionType type,
        @Size(max = 80) String tripId,
        @Size(max = 600) String coverImageUrl
) {}
