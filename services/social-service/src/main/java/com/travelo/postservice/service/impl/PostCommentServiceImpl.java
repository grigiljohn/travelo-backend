package com.travelo.postservice.service.impl;

import com.travelo.postservice.client.UserServiceClient;
import com.travelo.postservice.client.dto.UserDto;
import com.travelo.postservice.dto.PostCommentDto;
import com.travelo.postservice.dto.CreatePostCommentRequest;
import com.travelo.postservice.dto.events.CommentCreatedEvent;
import com.travelo.postservice.entity.Post;
import com.travelo.postservice.entity.PostComment;
import com.travelo.postservice.event.PostEventPublisher;
import com.travelo.postservice.exception.PostNotFoundException;
import com.travelo.postservice.realtime.CommentStreamBroker;
import com.travelo.postservice.realtime.CommentStreamEvent;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class PostCommentServiceImpl implements PostCommentService {

    private static final Logger logger = LoggerFactory.getLogger(PostCommentServiceImpl.class);

    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostEventPublisher postEventPublisher;
    private final UserServiceClient userServiceClient;
    private final CommentStreamBroker commentStreamBroker;
    // TODO: Add CommentLikeRepository for tracking individual comment likes

    public PostCommentServiceImpl(
            PostRepository postRepository,
            PostCommentRepository postCommentRepository,
            PostEventPublisher postEventPublisher,
            UserServiceClient userServiceClient,
            CommentStreamBroker commentStreamBroker) {
        this.postRepository = postRepository;
        this.postCommentRepository = postCommentRepository;
        this.postEventPublisher = postEventPublisher;
        this.userServiceClient = userServiceClient;
        this.commentStreamBroker = commentStreamBroker;
        logger.info("PostCommentServiceImpl initialized");
    }

    /**
     * Resolve (username, avatarUrl) for every distinct comment author in a single
     * sweep so a page of comments costs at most N user-service round-trips
     * (N = distinct authors). Failures are swallowed — caller just sees nulls
     * and the mobile client renders a generic "User" affordance.
     */
    private Map<String, UserLite> resolveAuthors(Set<String> userIds) {
        Map<String, UserLite> out = new HashMap<>();
        if (userIds == null || userIds.isEmpty() || userServiceClient == null) {
            return out;
        }
        for (String uid : userIds) {
            if (uid == null || uid.isBlank()) continue;
            try {
                UserDto u = userServiceClient.getUser(UUID.fromString(uid));
                if (u != null) {
                    out.put(uid, new UserLite(u.getUsername(), u.getProfilePictureUrl()));
                }
            } catch (Exception ex) {
                logger.debug("comment author lookup failed for {}: {}", uid, ex.toString());
            }
        }
        return out;
    }

    private record UserLite(String username, String avatarUrl) {}

    private PostCommentDto toDto(PostComment comment, List<PostCommentDto> replies,
                                 boolean isLiked, Map<String, UserLite> authors) {
        UserLite u = authors.get(comment.getUserId());
        return PostCommentDto.fromEntity(
                comment,
                replies,
                isLiked,
                u != null ? u.username() : null,
                u != null ? u.avatarUrl() : null
        );
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

        // Emit comment.created for realtime-service (notification to post owner) + analytics.
        try {
            postEventPublisher.publishCommentCreated(CommentCreatedEvent.of(
                    comment.getId(),
                    postId,
                    post.getUserId(),
                    userId,
                    request.parentId(),
                    request.commentText()
            ));
        } catch (Exception ex) {
            logger.warn("Failed to emit comment.created event for commentId={}: {}", comment.getId(), ex.toString());
        }

        logger.info("User {} commented on post {}", userId, postId);
        Map<String, UserLite> authors = resolveAuthors(Set.of(userId));
        PostCommentDto dto = toDto(comment, List.of(), false, authors);

        // Fan out to SSE subscribers *after* the JPA tx commits so that no
        // subscriber ever sees a comment that eventually rolls back. The
        // broker itself never throws into this thread, but we still guard
        // the registration to keep the comment write path bullet-proof.
        try {
            final String finalPostId = postId;
            final PostCommentDto finalDto = dto;
            org.springframework.transaction.support.TransactionSynchronizationManager
                    .registerSynchronization(new org.springframework.transaction.support
                            .TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            commentStreamBroker.publish(finalPostId, toStreamEvent(finalDto));
                        }
                    });
        } catch (IllegalStateException noTx) {
            // No active synchronization (e.g. called outside @Transactional in a test) —
            // publish directly; worst case the caller sees an event for a row that
            // immediately fails, which a well-behaved SSE client just ignores on refresh.
            commentStreamBroker.publish(postId, toStreamEvent(dto));
        }

        return dto;
    }

    private static CommentStreamEvent toStreamEvent(PostCommentDto dto) {
        return new CommentStreamEvent(
                dto.id() == null ? null : dto.id().toString(),
                dto.postId(),
                dto.userId(),
                dto.commentText(),
                dto.parentId() == null ? null : dto.parentId().toString(),
                dto.username(),
                dto.avatarUrl(),
                dto.createdAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostCommentDto> getComments(String postId, int page, int limit, String currentUserId) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<PostComment> comments = postCommentRepository.findTopLevelCommentsByPostId(postId, pageable);

        // Gather every distinct author (top-level + first 3 replies) in one pass so
        // resolveAuthors can issue at most one user-service call per unique id.
        Map<String, List<PostComment>> repliesByParent = new HashMap<>();
        Set<String> authorIds = new LinkedHashSet<>();
        for (PostComment c : comments.getContent()) {
            authorIds.add(c.getUserId());
            List<PostComment> replies = postCommentRepository.findRepliesByParentId(c.getId());
            repliesByParent.put(c.getId().toString(), replies);
            for (PostComment r : replies) authorIds.add(r.getUserId());
        }
        Map<String, UserLite> authors = resolveAuthors(authorIds);

        return comments.map(comment -> {
            List<PostComment> replies = repliesByParent.getOrDefault(comment.getId().toString(), List.of());
            List<PostCommentDto> replyDtos = replies.stream()
                    .limit(3)
                    .map(reply -> toDto(reply, List.of(), false, authors))
                    .collect(Collectors.toList());
            return toDto(comment, replyDtos, false, authors);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostCommentDto> getCommentReplies(UUID commentId, int page, int limit, String currentUserId) {
        postCommentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        Pageable pageable = PageRequest.of(page - 1, limit);
        List<PostComment> replies = postCommentRepository.findRepliesByParentId(commentId);

        Set<String> authorIds = replies.stream().map(PostComment::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, UserLite> authors = resolveAuthors(authorIds);

        List<PostCommentDto> replyDtos = replies.stream()
                .map(reply -> toDto(reply, List.of(), false, authors))
                .collect(Collectors.toList());

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
        return toDto(comment, List.of(), false, resolveAuthors(Set.of(comment.getUserId())));
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
        return toDto(comment, List.of(), liked, resolveAuthors(Set.of(comment.getUserId())));
    }

    @Override
    @Transactional(readOnly = true)
    public PostCommentDto getCommentById(UUID commentId, String currentUserId) {
        PostComment comment = postCommentRepository.findByIdAndNotDeleted(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        List<PostComment> replies = postCommentRepository.findRepliesByParentId(commentId);

        Set<String> authorIds = new LinkedHashSet<>();
        authorIds.add(comment.getUserId());
        for (PostComment r : replies) authorIds.add(r.getUserId());
        Map<String, UserLite> authors = resolveAuthors(authorIds);

        List<PostCommentDto> replyDtos = replies.stream()
                .map(reply -> toDto(reply, List.of(), false, authors))
                .collect(Collectors.toList());

        return toDto(comment, replyDtos, false, authors); // TODO: Check if liked
    }
}

