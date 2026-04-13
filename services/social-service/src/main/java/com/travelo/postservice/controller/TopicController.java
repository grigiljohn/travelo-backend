package com.travelo.postservice.controller;

import com.travelo.postservice.dto.ApiResponse;
import com.travelo.postservice.dto.TopicDto;
import com.travelo.postservice.service.TopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/topics")
public class TopicController {
    private static final Logger logger = LoggerFactory.getLogger(TopicController.class);
    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<TopicDto>>> getTrendingTopics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TopicDto> topics = topicService.getTrendingTopics(pageable);
            return ResponseEntity.ok(ApiResponse.success("Trending topics fetched successfully", topics.getContent()));
        } catch (Exception e) {
            logger.error("Error fetching trending topics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to fetch trending topics: " + e.getMessage(), "TOPICS_FETCH_FAILED"));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<TopicDto>>> searchTopics(@RequestParam String q) {
        try {
            List<TopicDto> topics = topicService.searchTopics(q);
            return ResponseEntity.ok(ApiResponse.success("Topics found", topics));
        } catch (Exception e) {
            logger.error("Error searching topics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to search topics: " + e.getMessage(), "TOPICS_SEARCH_FAILED"));
        }
    }

    @PostMapping("/{topicId}/associate")
    public ResponseEntity<ApiResponse<TopicDto>> associateTopicWithPost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String topicId,
            @RequestParam String postId) {
        try {
            String userId = jwt.getSubject();
            logger.info("Associating topic {} with post {} by user {}", topicId, postId, userId);
            TopicDto topic = topicService.associateTopicWithPost(postId, topicId);
            return ResponseEntity.ok(ApiResponse.success("Topic associated successfully", topic));
        } catch (Exception e) {
            logger.error("Error associating topic with post", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to associate topic: " + e.getMessage(), "TOPIC_ASSOCIATE_FAILED"));
        }
    }
}

