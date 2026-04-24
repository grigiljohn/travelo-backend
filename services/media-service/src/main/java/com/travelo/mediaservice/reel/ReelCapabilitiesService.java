package com.travelo.mediaservice.reel;

import com.travelo.mediaservice.dto.ReelCapabilitiesResponse;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ReelCapabilitiesService {

    private final ReelFilterService filterService;
    private final ReelMusicCatalogService musicCatalogService;

    public ReelCapabilitiesService(ReelFilterService filterService, ReelMusicCatalogService musicCatalogService) {
        this.filterService = filterService;
        this.musicCatalogService = musicCatalogService;
    }

    public ReelCapabilitiesResponse getCapabilities() {
        List<ReelCapabilitiesResponse.FilterPreset> filters = Arrays.stream(ReelFilterType.values())
                .map(this::toPreset)
                .toList();
        return new ReelCapabilitiesResponse(
                "720x1280",
                "9:16",
                5000,
                filters
        );
    }

    private ReelCapabilitiesResponse.FilterPreset toPreset(ReelFilterType type) {
        String label = switch (type) {
            case NONE -> "Original";
            case CINEMATIC -> "Cinematic";
            case VIBRANT -> "Vibrant";
            case COOL -> "Cool";
            case MONO -> "Mono";
            case SOFT_SKIN -> "Soft Skin";
            case DRAMATIC -> "Dramatic";
        };
        String description = switch (type) {
            case NONE -> "No grade, only normalization and encode.";
            case CINEMATIC -> "Warm tone, contrast boost, soft vignette.";
            case VIBRANT -> "Higher saturation and brighter highlights.";
            case COOL -> "Bluish tone with reduced warmth.";
            case MONO -> "Grayscale look.";
            case SOFT_SKIN -> "Slight blur and gentle brightness lift.";
            case DRAMATIC -> "High contrast with lower saturation.";
        };
        String ffmpeg = filterService.buildVideoFilter(type);
        String musicCategory = musicCatalogService.categoryForFilter(type).name();
        return new ReelCapabilitiesResponse.FilterPreset(
                type.name(),
                label,
                description,
                ffmpeg,
                musicCategory
        );
    }
}
