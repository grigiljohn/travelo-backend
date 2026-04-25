package com.travelo.mapservice.dto;

import java.util.List;

public record MapMediaResponseDto(
        String mode,
        double zoom,
        int total,
        List<MapMediaItemDto> items
) {}
