package com.travelo.reelservice.service;

import java.util.UUID;

/**
 * Service responsible for recording reel views. Split from {@link ReelService}
 * so the read/ranking path stays lean and the write path can add its own
 * dedupe / analytics concerns without polluting feed fetching.
 */
public interface ReelViewService {

    /**
     * Record that a user watched a reel.
     *
     * @param reelId  reel that was watched; if no reel matches this id the call
     *                is treated as a no-op (we never 404 a fire-and-forget
     *                analytics ping from the client).
     * @param userId  caller user id (string form — may be a UUID or a synthetic
     *                id depending on auth). Must not be blank.
     * @param viewDurationSeconds  how long the viewer actually watched, in
     *                             seconds. Null is accepted for legacy clients.
     * @param completionPercentage fraction 0.0 – 1.0 (or 0–100) indicating how
     *                             much of the reel was watched. Null is
     *                             accepted; we coerce to the ratio form in the
     *                             analytics payload.
     * @return {@code true} if the underlying view counter was bumped,
     *         {@code false} if the call was deduped or the reel did not exist.
     */
    boolean recordView(UUID reelId,
                       String userId,
                       Integer viewDurationSeconds,
                       Double completionPercentage);
}
