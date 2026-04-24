package com.travelo.postservice.service.impl;

import com.travelo.postservice.entity.PostComment;
import com.travelo.postservice.repository.PostCommentRepository;
import com.travelo.postservice.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Seeds mock comments into an empty comment table so local/dev instances show
 * realistic content without requiring a full database import. Disabled by
 * default — flip {@code app.dev.mock-data.enabled=true} in an environment
 * where you explicitly want seeded mocks (never in production).
 *
 * <p>Historically this ran unconditionally on every boot which made staging
 * ambiguous and blurred the "clean DB" signal. E22 retired that behavior;
 * the property-gated form is the permanent shape.</p>
 */
@Component
@ConditionalOnProperty(
        prefix = "app.dev.mock-data",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false)
public class MockDataInitializationService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(MockDataInitializationService.class);

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Autowired
    private PostRepository postRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // Only initialize if database is empty
        if (postCommentRepository.count() == 0) {
            logger.info("Initializing mock comments data...");
            initializeMockComments();
            logger.info("Mock comments data initialized");
        }
    }

    private void initializeMockComments() {
        // Get first post if exists, otherwise use a dummy post ID
        String postId = postRepository.findAll().stream()
                .findFirst()
                .map(post -> post.getId())
                .orElse("mock-post-1");

        List<PostComment> mockComments = List.of(
                createMockComment(postId, "user-1", "jane_doe", "Amazing photo! 😍", 12),
                createMockComment(postId, "user-2", "travel_lover", "Where is this taken? Looks incredible!", 8),
                createMockComment(postId, "user-3", "photography_enthusiast", "The lighting is perfect! Great shot 📸", 15)
        );

        postCommentRepository.saveAll(mockComments);
    }

    private PostComment createMockComment(String postId, String userId, String username, String text, int likes) {
        PostComment comment = new PostComment(postId, userId, text);
        comment.setId(UUID.randomUUID());
        comment.setLikeCount(likes);
        comment.setCreatedAt(OffsetDateTime.now().minusHours(2 + (int)(Math.random() * 6)));
        comment.setUpdatedAt(comment.getCreatedAt());
        return comment;
    }
}

