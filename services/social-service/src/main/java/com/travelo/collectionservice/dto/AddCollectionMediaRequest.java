package com.travelo.collectionservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddCollectionMediaRequest(
        @NotEmpty List<@Valid AddCollectionMediaItemRequest> items
) {}
