package com.travelo.reelservice.repository;

import com.travelo.reelservice.entity.ReelLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReelLikeRepository extends JpaRepository<ReelLike, UUID> {
    
    Optional<ReelLike> findByReelIdAndUserId(UUID reelId, String userId);
    
    long countByReelId(UUID reelId);
    
    boolean existsByReelIdAndUserId(UUID reelId, String userId);
}

