package com.travelo.postservice.repository;

import com.travelo.postservice.entity.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, String> {
    Optional<Topic> findByName(String name);
    
    List<Topic> findByIsActiveTrueOrderByTrendScoreDesc();
    
    Page<Topic> findByIsActiveTrueOrderByTrendScoreDesc(Pageable pageable);
    
    @Query("SELECT t FROM Topic t WHERE t.isActive = true AND LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY t.trendScore DESC")
    List<Topic> searchTopics(String query);
}

