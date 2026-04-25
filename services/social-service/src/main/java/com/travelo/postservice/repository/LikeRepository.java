package com.travelo.postservice.repository;

import com.travelo.postservice.entity.Like;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserIdAndPostId(String userId, String postId);
    boolean existsByUserIdAndPostId(String userId, String postId);
    /**
     * Batch: likes the viewer has on a set of posts (e.g. feed enrichment).
     */
    List<Like> findByUserIdAndPostIdIn(String userId, List<String> postIds);
    void deleteByUserIdAndPostId(String userId, String postId);
    long countByPostId(String postId);

    /**
     * All likes for {@code userId}, most recent first. Used by the profile
     * Likes tab to reconstruct the user's like history paginated by like-time.
     */
    Page<Like> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /** Likes on a post, newest first (for "who liked" UI). */
    Page<Like> findByPostIdOrderByCreatedAtDesc(String postId, Pageable pageable);
}

