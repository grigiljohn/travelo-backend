package com.travelo.postservice.repository;

import com.travelo.postservice.entity.MediaItem;
import com.travelo.postservice.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaItemRepository extends JpaRepository<MediaItem, Long> {
    @Query("SELECT m FROM MediaItem m WHERE m.post.id = :postId ORDER BY m.position ASC")
    List<MediaItem> findByPostIdOrderByPositionAsc(@Param("postId") String postId);
    
    List<MediaItem> findByPostOrderByPositionAsc(Post post);
    
    @Modifying
    @Query("DELETE FROM MediaItem m WHERE m.post.id = :postId")
    void deleteByPostId(@Param("postId") String postId);
    
    void deleteByPost(Post post);
}

