package com.travelo.mediaservice.reel;

import org.springframework.stereotype.Service;

/**
 * FFmpeg eq/curves/vignette presets tuned for short vertical video.
 */
@Service
public class ReelFilterServiceImpl implements ReelFilterService {

    @Override
    public String buildVideoFilter(ReelFilterType type) {
        if (type == null || type == ReelFilterType.NONE) {
            return "";
        }
        return switch (type) {
            case CINEMATIC ->
                    "eq=contrast=1.12:brightness=0.03:saturation=1.08,vignette=angle=PI/5";
            case VIBRANT ->
                    "eq=contrast=1.05:brightness=0.06:saturation=1.48";
            case COOL ->
                    "eq=saturation=0.92:gamma=1.02,colorbalance=rs=-0.08:gs=-0.02:bs=0.12";
            case MONO -> "hue=s=0";
            case SOFT_SKIN ->
                    "gblur=sigma=0.85,eq=brightness=0.05:contrast=1.02";
            case DRAMATIC ->
                    "eq=contrast=1.38:brightness=-0.02:saturation=0.68";
            default -> "";
        };
    }
}
