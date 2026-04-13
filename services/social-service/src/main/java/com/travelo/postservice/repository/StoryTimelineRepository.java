package com.travelo.postservice.repository;

import com.travelo.postservice.entity.StoryTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoryTimelineRepository extends JpaRepository<StoryTimeline, String> {
    List<StoryTimeline> findByUserIdOrderByCreatedAtDesc(String userId);
    
    Optional<StoryTimeline> findByIdAndUserId(String id, String userId);
}

