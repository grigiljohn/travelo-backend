package com.travelo.postservice.repository;

import com.travelo.postservice.entity.PostTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface PostTopicRepository extends JpaRepository<PostTopic, String> {
    Optional<PostTopic> findByPostIdAndTopicId(String postId, String topicId);
    
    long countByTopicId(String topicId);
    
    @Query("SELECT COUNT(pt) FROM PostTopic pt WHERE pt.topic.id = :topicId AND pt.createdAt > :after")
    long countByTopicIdAndCreatedAtAfter(@Param("topicId") String topicId, @Param("after") OffsetDateTime after);
}

