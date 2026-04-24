package com.travelo.postservice.repository;

import com.travelo.postservice.entity.Post;
import com.travelo.postservice.entity.enums.MoodType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, String> {
    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Post> findByIdAndDeletedAtIsNull(@Param("id") String id);
    
    @Query("SELECT p FROM Post p WHERE p.deletedAt IS NULL")
    Page<Post> findByDeletedAtIsNull(Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.mood = :mood AND p.deletedAt IS NULL")
    Page<Post> findByMoodAndDeletedAtIsNull(@Param("mood") MoodType mood, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.userId = :userId AND p.deletedAt IS NULL")
    Page<Post> findByUserIdAndDeletedAtIsNull(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.userId IN :userIds AND p.deletedAt IS NULL")
    Page<Post> findByUserIdInAndDeletedAtIsNull(@Param("userIds") List<String> userIds, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.userId IN :userIds AND p.mood = :mood AND p.deletedAt IS NULL")
    Page<Post> findByUserIdInAndMoodAndDeletedAtIsNull(
            @Param("userIds") List<String> userIds,
            @Param("mood") MoodType mood,
            Pageable pageable);
    
    /**
     * Batch-load non-deleted posts by id. Order is not preserved here — callers that
     * need a specific order (e.g. saved-posts by save-time) must re-sort the result.
     */
    @Query("SELECT p FROM Post p WHERE p.id IN :ids AND p.deletedAt IS NULL")
    List<Post> findByIdInAndDeletedAtIsNull(@Param("ids") List<String> ids);

    // Debug method - count all posts (including deleted)
    long count();
    
    // Debug method - count non-deleted posts
    @Query("SELECT COUNT(p) FROM Post p WHERE p.deletedAt IS NULL")
    long countByDeletedAtIsNull();
}
