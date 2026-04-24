package com.travelo.mediaservice.reel;

/**
 * Curated library (URLs) + mapping from reel filter → backing track category.
 */
public interface ReelMusicCatalogService {

    ReelMusicTrack pickRandom(ReelMusicCategory category);

    /**
     * Auto music selection when client enables music without manual track.
     */
    ReelMusicCategory categoryForFilter(ReelFilterType filter);
}
