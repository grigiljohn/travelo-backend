package com.travelo.storyservice.repository;

import com.travelo.storyservice.entity.StoryHighlight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoryHighlightRepository extends JpaRepository<StoryHighlight, UUID> {
    
    /**
     * Find all highlights for a user.
     */
    @Query("SELECT sh FROM StoryHighlight sh WHERE sh.userId = :userId ORDER BY sh.createdAt DESC")
    List<StoryHighlight> findByUserId(@Param("userId") String userId);
}

