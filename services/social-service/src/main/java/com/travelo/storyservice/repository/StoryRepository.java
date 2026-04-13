package com.travelo.storyservice.repository;

import com.travelo.storyservice.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StoryRepository extends JpaRepository<Story, UUID> {
    
    /**
     * Find active (non-expired) stories for a user.
     */
    @Query("SELECT s FROM Story s WHERE s.userId = :userId AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    List<Story> findActiveStoriesByUserId(@Param("userId") String userId, @Param("now") OffsetDateTime now);
    
    /**
     * Find all active stories from followed users.
     */
    @Query("SELECT s FROM Story s WHERE s.userId IN :userIds AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    List<Story> findActiveStoriesByUserIds(@Param("userIds") List<String> userIds, @Param("now") OffsetDateTime now);

    /**
     * Discover stories for stories strip (all active stories).
     */
    @Query("SELECT s FROM Story s WHERE s.expiresAt > :now ORDER BY s.createdAt DESC")
    List<Story> findDiscoverStories(@Param("now") OffsetDateTime now);
    
    /**
     * Find expired stories (for cleanup).
     */
    @Query("SELECT s FROM Story s WHERE s.expiresAt < :now AND s.isHighlight = false")
    List<Story> findExpiredStories(@Param("now") OffsetDateTime now);
    
    /**
     * Find stories in a highlight.
     */
    @Query("SELECT s FROM Story s WHERE s.highlightId = :highlightId ORDER BY s.createdAt DESC")
    List<Story> findStoriesByHighlightId(@Param("highlightId") UUID highlightId);
    
    /**
     * Count active stories for a user.
     */
    @Query("SELECT COUNT(s) FROM Story s WHERE s.userId = :userId AND s.expiresAt > :now")
    long countActiveStoriesByUserId(@Param("userId") String userId, @Param("now") OffsetDateTime now);
}

