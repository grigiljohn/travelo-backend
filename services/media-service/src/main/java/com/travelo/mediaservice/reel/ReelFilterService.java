package com.travelo.mediaservice.reel;

/**
 * Maps {@link ReelFilterType} to FFmpeg {@code -vf} filter graphs (before 9:16 scaling).
 */
public interface ReelFilterService {

    /**
     * @return filter chain without surrounding quotes; empty for {@link ReelFilterType#NONE}.
     */
    String buildVideoFilter(ReelFilterType type);
}
