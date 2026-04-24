package com.travelo.discoveryservice.curated;

import java.util.List;
import java.util.Optional;

/**
 * Port for curated-collection reads. The implementation today is
 * config-backed (see {@link CuratedCollectionsServiceImpl}); if we later
 * persist collections in a DB or CMS, only the implementation changes —
 * callers and the HTTP contract stay the same.
 */
public interface CuratedCollectionsService {

    /**
     * Return all configured collections as lightweight row DTOs. The
     * {@code noteCount} is derived from the search index so it stays in
     * sync with the detail page's first page without a second round-trip.
     *
     * @param viewerId optional user id for personalization / privacy
     *                 filters on the count query
     */
    List<CuratedCollectionDto> listCollections(String viewerId);

    /**
     * Expand a single collection with a paged window of matching notes.
     * Returns {@link Optional#empty()} when no collection with that id
     * exists in the active configuration.
     */
    Optional<CuratedCollectionDetailDto> getCollectionDetail(
            String id,
            int page,
            int limit,
            String viewerId);
}
