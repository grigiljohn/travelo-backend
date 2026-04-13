package com.travelo.postservice.repository;

import com.travelo.postservice.entity.PostComment;
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
public interface PostCommentRepository extends JpaRepository<PostComment, UUID> {
    
    @Query("SELECT pc FROM PostComment pc WHERE pc.postId = :postId AND pc.parentId IS NULL AND pc.deletedAt IS NULL ORDER BY pc.createdAt DESC")
    Page<PostComment> findTopLevelCommentsByPostId(@Param("postId") String postId, Pageable pageable);
    
    @Query("SELECT pc FROM PostComment pc WHERE pc.parentId = :parentId AND pc.deletedAt IS NULL ORDER BY pc.createdAt ASC")
    List<PostComment> findRepliesByParentId(@Param("parentId") UUID parentId);
    
    @Query("SELECT COUNT(pc) FROM PostComment pc WHERE pc.postId = :postId AND pc.deletedAt IS NULL")
    long countByPostId(@Param("postId") String postId);
    
    @Query("SELECT pc FROM PostComment pc WHERE pc.id = :id AND pc.deletedAt IS NULL")
    Optional<PostComment> findByIdAndNotDeleted(@Param("id") UUID id);
    
    @Query("SELECT pc FROM PostComment pc WHERE pc.postId = :postId AND pc.userId = :userId AND pc.deletedAt IS NULL ORDER BY pc.createdAt DESC")
    List<PostComment> findByPostIdAndUserId(@Param("postId") String postId, @Param("userId") String userId);
}

