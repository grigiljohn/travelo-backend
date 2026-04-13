package com.travelo.userservice.repository;

import com.travelo.userservice.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {
    
    /**
     * Check if a user is following another user
     */
    boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);
    
    /**
     * Find follow relationship by follower and followee
     */
    Optional<Follow> findByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);
    
    /**
     * Count followers for a user
     */
    long countByFolloweeId(UUID followeeId);
    
    /**
     * Count users a user is following
     */
    long countByFollowerId(UUID followerId);
    
    /**
     * Delete follow relationship
     */
    void deleteByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);
    
    /**
     * Find all follow relationships for a follower
     */
    List<Follow> findByFollowerId(UUID followerId);

    /**
     * Users who follow [followeeId] (newest first when pageable sort is by createdAt desc).
     */
    Page<Follow> findByFolloweeId(UUID followeeId, Pageable pageable);

    /**
     * Users that [followerId] follows.
     */
    Page<Follow> findByFollowerId(UUID followerId, Pageable pageable);
}

