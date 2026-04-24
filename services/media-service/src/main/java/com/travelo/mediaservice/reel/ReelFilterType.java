package com.travelo.mediaservice.reel;

/**
 * Preset visual filters for reel delivery (FFmpeg vf chains).
 */
public enum ReelFilterType {
    NONE,
    CINEMATIC,
    VIBRANT,
    COOL,
    MONO,
    SOFT_SKIN,
    DRAMATIC;

    public static ReelFilterType fromParam(String raw) {
        if (raw == null || raw.isBlank()) {
            return NONE;
        }
        try {
            return ReelFilterType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
