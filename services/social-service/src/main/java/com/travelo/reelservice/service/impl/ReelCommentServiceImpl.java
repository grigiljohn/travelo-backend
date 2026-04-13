package com.travelo.reelservice.service.impl;

import com.travelo.reelservice.client.AnalyticsServiceClient;
import com.travelo.reelservice.dto.ReelCommentDto;
import com.travelo.reelservice.entity.Reel;
import com.travelo.reelservice.entity.ReelComment;
import com.travelo.reelservice.exception.ReelNotFoundException;
import com.travelo.reelservice.repository.ReelCommentRepository;
import com.travelo.reelservice.repository.ReelRepository;
import com.travelo.reelservice.service.ReelCommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(transactionManager = "reelTransactionManager")
public class ReelCommentServiceImpl implements ReelCommentService {

    private static final Logger logger = LoggerFactory.getLogger(ReelCommentServiceImpl.class);

    private final ReelRepository reelRepository;
    private final ReelCommentRepository reelCommentRepository;
    private final AnalyticsServiceClient analyticsServiceClient;

    public ReelCommentServiceImpl(ReelRepository reelRepository,
                                 ReelCommentRepository reelCommentRepository,
                                 AnalyticsServiceClient analyticsServiceClient) {
        this.reelRepository = reelRepository;
        this.reelCommentRepository = reelCommentRepository;
        this.analyticsServiceClient = analyticsServiceClient;
    }

    @Override
    public ReelCommentDto addComment(UUID reelId, String userId, String commentText, UUID parentId) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new ReelNotFoundException(reelId));

        ReelComment comment = new ReelComment(reelId, userId, commentText);
        if (parentId != null) {
            comment.setParentId(parentId);
        }

        comment = reelCommentRepository.save(comment);

        // Update comment count
        reel.setCommentCount(reel.getCommentCount() + 1);
        reelRepository.save(reel);

        // Track analytics event
        analyticsServiceClient.trackReelComment(reelId, userId);

        logger.info("User {} commented on reel {}", userId, reelId);
        return ReelCommentDto.fromEntity(comment);
    }

    @Override
    @Transactional(transactionManager = "reelTransactionManager", readOnly = true)
    public Page<ReelCommentDto> getComments(UUID reelId, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<ReelComment> comments = reelCommentRepository.findTopLevelCommentsByReelId(reelId, pageable);
        
        return comments.map(ReelCommentDto::fromEntity);
    }
}

