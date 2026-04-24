package com.travelo.reelservice.repository;

import com.travelo.reelservice.entity.Reel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReelRepository extends JpaRepository<Reel, UUID> {
    
    Optional<Reel> findByPostId(String postId);
    
    @Query("SELECT r FROM Reel r WHERE r.status = 'READY' ORDER BY r.rankingScore DESC NULLS LAST, r.createdAt DESC")
    Page<Reel> findReadyReelsOrderedByRanking(Pageable pageable);
    
    @Query("SELECT r FROM Reel r WHERE r.userId = :userId AND r.status = 'READY' ORDER BY r.createdAt DESC")
    List<Reel> findReadyReelsByUserId(@Param("userId") String userId);
    
    @Query("SELECT r FROM Reel r WHERE r.status = :status")
    List<Reel> findByStatus(@Param("status") Reel.Status status);
    
    @Query("SELECT r FROM Reel r WHERE r.transcodingStatus = :transcodingStatus")
    List<Reel> findByTranscodingStatus(@Param("transcodingStatus") Reel.TranscodingStatus transcodingStatus);
    
    /**
     * Find reel by media ID (for transcoding updates).
     */
    @Query("SELECT r FROM Reel r WHERE r.mediaId = :mediaId")
    Optional<Reel> findByMediaId(@Param("mediaId") UUID mediaId);

    /**
     * Atomically bump the view counter. Returns the number of rows updated
     * (0 if no reel with that id exists, 1 on success). Using a single
     * JPQL UPDATE avoids read-modify-write races when many concurrent
     * viewers watch the same reel.
     */
    @Modifying
    @Query("UPDATE Reel r SET r.viewCount = r.viewCount + 1, r.updatedAt = CURRENT_TIMESTAMP WHERE r.id = :id")
    int incrementViewCount(@Param("id") UUID id);
}

