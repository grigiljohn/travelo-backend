package com.travelo.storyservice.repository;

import com.travelo.storyservice.entity.StoryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoryViewRepository extends JpaRepository<StoryView, UUID> {
    
    /**
     * Find all views for a story.
     */
    @Query("SELECT sv FROM StoryView sv WHERE sv.storyId = :storyId ORDER BY sv.viewedAt DESC")
    List<StoryView> findByStoryId(@Param("storyId") UUID storyId);
    
    /**
     * Check if a user has already viewed a story.
     */
    Optional<StoryView> findByStoryIdAndUserId(UUID storyId, String userId);
    
    /**
     * Count views for a story.
     */
    long countByStoryId(UUID storyId);
}

