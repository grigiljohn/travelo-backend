package com.travelo.postservice.repository;

import com.travelo.postservice.entity.Post;
import com.travelo.postservice.entity.PostTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostTagRepository extends JpaRepository<PostTag, Long> {
    @Query("SELECT t FROM PostTag t WHERE t.post.id = :postId")
    List<PostTag> findByPostId(@Param("postId") String postId);
    
    List<PostTag> findByPost(Post post);
    
    @Modifying
    @Query("DELETE FROM PostTag t WHERE t.post.id = :postId")
    void deleteByPostId(@Param("postId") String postId);
    
    void deleteByPost(Post post);

    /**
     * Number of existing rows for a given tag name. Used to detect first-use of a tag
     * so we can emit a `tag.created` event to downstream search/trending indexers.
     */
    long countByTag(String tag);
}

