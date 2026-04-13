package com.travelo.postservice.service.impl;

import com.travelo.postservice.dto.TopicDto;
import com.travelo.postservice.entity.Topic;
import com.travelo.postservice.entity.PostTopic;
import com.travelo.postservice.repository.TopicRepository;
import com.travelo.postservice.repository.PostTopicRepository;
import com.travelo.postservice.service.TopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TopicServiceImpl implements TopicService {
    private static final Logger logger = LoggerFactory.getLogger(TopicServiceImpl.class);
    
    private final TopicRepository topicRepository;
    private final PostTopicRepository postTopicRepository;
    
    public TopicServiceImpl(TopicRepository topicRepository, PostTopicRepository postTopicRepository) {
        this.topicRepository = topicRepository;
        this.postTopicRepository = postTopicRepository;
    }
    
    @Override
    public List<TopicDto> getTrendingTopics() {
        return topicRepository.findByIsActiveTrueOrderByTrendScoreDesc().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public Page<TopicDto> getTrendingTopics(Pageable pageable) {
        return topicRepository.findByIsActiveTrueOrderByTrendScoreDesc(pageable)
            .map(this::toDto);
    }
    
    @Override
    public List<TopicDto> searchTopics(String query) {
        return topicRepository.searchTopics(query).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public TopicDto createTopic(String name, String description) {
        Topic topic = Topic.builder()
            .name(name)
            .description(description)
            .build();
        topic = topicRepository.save(topic);
        return toDto(topic);
    }
    
    @Override
    @Transactional
    public TopicDto associateTopicWithPost(String postId, String topicId) {
        Topic topic = topicRepository.findById(topicId)
            .orElseThrow(() -> new RuntimeException("Topic not found: " + topicId));
        
        // Check if association already exists
        if (postTopicRepository.findByPostIdAndTopicId(postId, topicId).isEmpty()) {
            PostTopic postTopic = PostTopic.builder()
                .postId(postId)
                .topic(topic)
                .build();
            postTopicRepository.save(postTopic);
            
            // Update topic post count
            topic.setPostCount(topic.getPostCount() + 1);
            topicRepository.save(topic);
        }
        
        return toDto(topic);
    }
    
    @Override
    @Transactional
    public void updateTopicTrendScore(String topicId) {
        Topic topic = topicRepository.findById(topicId)
            .orElseThrow(() -> new RuntimeException("Topic not found: " + topicId));
        
        // Simple trend score calculation: post_count * 0.5 + recent_activity
        long recentPosts = postTopicRepository.countByTopicIdAndCreatedAtAfter(
            topicId, java.time.OffsetDateTime.now().minusDays(7));
        double trendScore = topic.getPostCount() * 0.5 + recentPosts * 2.0;
        
        topic.setTrendScore(java.math.BigDecimal.valueOf(trendScore));
        topicRepository.save(topic);
    }
    
    private TopicDto toDto(Topic topic) {
        return new TopicDto(
            topic.getId(),
            topic.getName(),
            topic.getDescription(),
            topic.getTrendScore(),
            topic.getPostCount(),
            topic.getIsActive(),
            topic.getCreatedAt()
        );
    }
}

