package com.travelo.mediaservice.reel;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public interface ReelProcessingService {

    /**
     * Trim → filter → 720×1280 9:16 → optional library music mux → H.264/AAC MP4.
     *
     * @param progressJobId optional client-supplied key used to report pipeline stages
     *                      into {@link ReelJobProgressTracker}. When {@code null} or blank,
     *                      no progress is reported.
     * @return processed MP4 file (caller must delete); throws on hard failure
     */
    File processReelDelivery(File inputVideo,
                             UUID jobId,
                             ReelFilterType filterType,
                             boolean musicEnabled,
                             String progressJobId)
            throws IOException, InterruptedException;
}
