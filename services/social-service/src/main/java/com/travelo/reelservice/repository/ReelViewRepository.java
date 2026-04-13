package com.travelo.reelservice.repository;

import com.travelo.reelservice.entity.ReelView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReelViewRepository extends JpaRepository<ReelView, UUID> {
    
    long countByReelId(UUID reelId);
    
    @Query("SELECT AVG(rv.completionPercentage) FROM ReelView rv WHERE rv.reelId = :reelId")
    Double getAverageCompletionPercentage(@Param("reelId") UUID reelId);
}

