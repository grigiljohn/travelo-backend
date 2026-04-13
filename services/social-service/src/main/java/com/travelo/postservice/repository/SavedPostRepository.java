package com.travelo.postservice.repository;

import com.travelo.postservice.entity.SavedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedPostRepository extends JpaRepository<SavedPost, UUID> {
    
    Optional<SavedPost> findByUserIdAndPostIdAndCollectionName(String userId, String postId, String collectionName);
    
    Page<SavedPost> findByUserIdAndCollectionNameOrderByCreatedAtDesc(String userId, String collectionName, Pageable pageable);
    
    Page<SavedPost> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    @Query("SELECT DISTINCT sp.collectionName FROM SavedPost sp WHERE sp.userId = :userId ORDER BY sp.collectionName")
    List<String> findDistinctCollectionNamesByUserId(@Param("userId") String userId);
    
    long countByUserIdAndCollectionName(String userId, String collectionName);
    
    boolean existsByUserIdAndPostId(String userId, String postId);
}

