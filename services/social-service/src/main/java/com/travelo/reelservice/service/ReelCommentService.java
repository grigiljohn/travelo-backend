package com.travelo.reelservice.service;

import com.travelo.reelservice.dto.ReelCommentDto;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * Service for reel comments.
 */
public interface ReelCommentService {
    
    /**
     * Add a comment to a reel.
     */
    ReelCommentDto addComment(UUID reelId, String userId, String commentText, UUID parentId);
    
    /**
     * Get comments for a reel (paginated).
     */
    Page<ReelCommentDto> getComments(UUID reelId, int page, int limit);
}

