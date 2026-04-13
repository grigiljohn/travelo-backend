package com.travelo.postservice.service.impl;

import com.travelo.postservice.dto.PostCommentDto;
import com.travelo.postservice.dto.CreatePostCommentRequest;
import com.travelo.postservice.entity.Post;
import com.travelo.postservice.entity.PostComment;
import com.travelo.postservice.exception.PostNotFoundException;
import com.travelo.postservice.repository.PostRepository;
import com.travelo.postservice.repository.PostCommentRepository;
import com.travelo.postservice.service.PostCommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;

@Service
@Transactional
public class PostCommentServiceImpl implements PostCommentService {

    private static final Logger logger = LoggerFactory.getLogger(PostCommentServiceImpl.class);

    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;
    // TODO: Add CommentLikeRepository for tracking individual comment likes

    public PostCommentServiceImpl(
            PostRepository postRepository,
            PostCommentRepository postCommentRepository) {
        this.postRepository = postRepository;
        this.postCommentRepository = postCommentRepository;
        logger.info("PostCommentServiceImpl initialized");
    }

    @Override
    @Transactional
    public PostCommentDto addComment(String postId, String userId, CreatePostCommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        PostComment comment = new PostComment(postId, userId, request.commentText());
        if (request.parentId() != null) {
            // Validate parent comment exists
            PostComment parent = postCommentRepository.findByIdAndNotDeleted(request.parentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));
            comment.setParentId(request.parentId());
        }

        comment = postCommentRepository.save(comment);

        // Update comment count on post
        post.setComments(post.getComments() + 1);
        postRepository.save(post);

        // TODO: Send notification to post owner
        // TODO: Track analytics event

        logger.info("User {} commented on post {}", userId, postId);
        return PostCommentDto.fromEntity(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostCommentDto> getComments(String postId, int page, int limit, String currentUserId) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<PostComment> comments = postCommentRepository.findTopLevelCommentsByPostId(postId, pageable);
        
        // If no comments found, return mock data for development
        if (comments.isEmpty() && page == 1) {
            logger.info("No comments found for post {}, returning mock data", postId);
            return getMockComments(postId, pageable);
        }
        
        // Load replies for each comment (could be optimized with batch loading)
        return comments.map(comment -> {
            List<PostComment> replies = postCommentRepository.findRepliesByParentId(comment.getId());
            List<PostCommentDto> replyDtos = replies.stream()
                    .map(reply -> PostCommentDto.fromEntity(reply, null, false)) // TODO: Check if liked
                    .limit(3) // Limit replies preview
                    .collect(Collectors.toList());
            return PostCommentDto.fromEntity(comment, replyDtos, false); // TODO: Check if liked
        });
    }
    
    /**
     * Return mock comments when database is empty.
     * This provides fallback data for development.
     */
    private Page<PostCommentDto> getMockComments(String postId, Pageable pageable) {
        List<PostCommentDto> mockComments = List.of(
            new PostCommentDto(
                UUID.randomUUID(),
                postId,
                "user-1",
                "Amazing photo! 😍",
                null,
                12,
                OffsetDateTime.now().minusHours(2),
                OffsetDateTime.now().minusHours(2),
                List.of(),
                false,
                "jane_doe",
                "https://i.pravatar.cc/150?img=1"
            ),
            new PostCommentDto(
                UUID.randomUUID(),
                postId,
                "user-2",
                "Where is this taken? Looks incredible!",
                null,
                8,
                OffsetDateTime.now().minusHours(5),
                OffsetDateTime.now().minusHours(5),
                List.of(),
                true,
                "travel_lover",
                "https://i.pravatar.cc/150?img=2"
            ),
            new PostCommentDto(
                UUID.randomUUID(),
                postId,
                "user-3",
                "The lighting is perfect! Great shot 📸",
                null,
                15,
                OffsetDateTime.now().minusHours(8),
                OffsetDateTime.now().minusHours(8),
                List.of(),
                false,
                "photography_enthusiast",
                "https://i.pravatar.cc/150?img=3"
            )
        );
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), mockComments.size());
        List<PostCommentDto> pagedComments = mockComments.subList(start, end);
        
        return new org.springframework.data.domain.PageImpl<>(
            pagedComments,
            pageable,
            mockComments.size()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostCommentDto> getCommentReplies(UUID commentId, int page, int limit, String currentUserId) {
        PostComment parentComment = postCommentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        Pageable pageable = PageRequest.of(page - 1, limit);
        List<PostComment> replies = postCommentRepository.findRepliesByParentId(commentId);
        
        // Convert to DTOs
        List<PostCommentDto> replyDtos = replies.stream()
                .map(reply -> PostCommentDto.fromEntity(reply, null, false))
                .collect(Collectors.toList());

        // Create a page from the list
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), replyDtos.size());
        List<PostCommentDto> pagedReplies = replyDtos.subList(start, end);
        
        return new org.springframework.data.domain.PageImpl<>(
                pagedReplies,
                pageable,
                replies.size()
        );
    }

    @Override
    @Transactional
    public PostCommentDto updateComment(UUID commentId, String userId, String commentText) {
        PostComment comment = postCommentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (!comment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only edit your own comments");
        }

        comment.setCommentText(commentText);
        comment = postCommentRepository.save(comment);

        logger.info("Comment {} updated by user {}", commentId, userId);
        return PostCommentDto.fromEntity(comment);
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId, String userId) {
        PostComment comment = postCommentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        // Check if user owns the comment or the post
        Post post = postRepository.findById(comment.getPostId())
                .orElseThrow(() -> new PostNotFoundException(comment.getPostId()));

        if (!comment.getUserId().equals(userId) && !post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("You don't have permission to delete this comment");
        }

        // Soft delete
        comment.setDeletedAt(OffsetDateTime.now());
        postCommentRepository.save(comment);

        // Update comment count on post
        post.setComments(Math.max(0, post.getComments() - 1));
        postRepository.save(post);

        logger.info("Comment {} deleted by user {}", commentId, userId);
    }

    @Override
    @Transactional
    public PostCommentDto likeComment(UUID commentId, String userId, boolean liked) {
        PostComment comment = postCommentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        // TODO: Track individual likes in CommentLikeRepository
        // For now, just increment/decrement like count
        if (liked) {
            comment.setLikeCount(comment.getLikeCount() + 1);
        } else {
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
        }
        comment = postCommentRepository.save(comment);

        logger.info("Comment {} {} by user {}", commentId, liked ? "liked" : "unliked", userId);
        return PostCommentDto.fromEntity(comment, null, liked);
    }

    @Override
    @Transactional(readOnly = true)
    public PostCommentDto getCommentById(UUID commentId, String currentUserId) {
        PostComment comment = postCommentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        List<PostComment> replies = postCommentRepository.findRepliesByParentId(commentId);
        List<PostCommentDto> replyDtos = replies.stream()
                .map(reply -> PostCommentDto.fromEntity(reply, null, false))
                .collect(Collectors.toList());

        return PostCommentDto.fromEntity(comment, replyDtos, false); // TODO: Check if liked
    }
}

