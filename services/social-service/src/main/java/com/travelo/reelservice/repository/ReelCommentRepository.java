package com.travelo.reelservice.repository;

import com.travelo.reelservice.entity.ReelComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReelCommentRepository extends JpaRepository<ReelComment, UUID> {
    
    @Query("SELECT rc FROM ReelComment rc WHERE rc.reelId = :reelId AND rc.parentId IS NULL ORDER BY rc.createdAt DESC")
    Page<ReelComment> findTopLevelCommentsByReelId(@Param("reelId") UUID reelId, Pageable pageable);
    
    @Query("SELECT rc FROM ReelComment rc WHERE rc.parentId = :parentId ORDER BY rc.createdAt ASC")
    List<ReelComment> findRepliesByParentId(@Param("parentId") UUID parentId);
    
    long countByReelId(UUID reelId);
}

