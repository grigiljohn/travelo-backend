package com.travelo.musicservice.repository;

import com.travelo.musicservice.entity.MusicTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MusicTrackRepository extends JpaRepository<MusicTrack, UUID> {

    /**
     * Find recommended music tracks
     */
    List<MusicTrack> findByIsRecommendedTrueAndIsActiveTrueOrderByPlayCountDesc();

    /**
     * Find music tracks by mood
     */
    List<MusicTrack> findByMoodAndIsActiveTrueOrderByPlayCountDesc(String mood);

    /**
     * Search music tracks by name or artist
     */
    @Query("SELECT m FROM MusicTrack m WHERE m.isActive = true AND " +
           "(LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.artist) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY m.playCount DESC")
    List<MusicTrack> searchTracks(@Param("query") String query);

    /**
     * Find all active tracks
     */
    List<MusicTrack> findByIsActiveTrueOrderByPlayCountDesc();
}

