package com.travelo.storyservice.repository;

import com.travelo.storyservice.entity.StoryReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoryReplyRepository extends JpaRepository<StoryReply, UUID> {
    
    /**
     * Find all replies for a story.
     */
    @Query("SELECT sr FROM StoryReply sr WHERE sr.storyId = :storyId ORDER BY sr.createdAt ASC")
    List<StoryReply> findByStoryId(@Param("storyId") UUID storyId);
    
    /**
     * Count replies for a story.
     */
    long countByStoryId(UUID storyId);
}

