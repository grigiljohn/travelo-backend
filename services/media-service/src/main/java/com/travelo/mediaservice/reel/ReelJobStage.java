package com.travelo.mediaservice.reel;

/**
 * Coarse-grained stages that the mobile client can render as real processing progress.
 * Order matches the pipeline execution order.
 */
public enum ReelJobStage {
    /** Job registered, waiting for FFmpeg to pick it up. */
    QUEUED,
    /** Probing + smart-trim of the source clip. */
    OPTIMIZING,
    /** FFmpeg filter graph + 9:16 scale + H.264 encode. */
    FILTERING,
    /** Optional: download track and mux audio. */
    MUSIC,
    /** Uploading processed MP4 to storage + thumbnail registration. */
    FINALIZING,
    /** Pipeline finished successfully. */
    READY,
    /** Terminal error; client should fall back or retry. */
    FAILED;
}
