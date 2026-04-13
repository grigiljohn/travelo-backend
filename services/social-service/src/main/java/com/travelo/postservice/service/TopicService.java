package com.travelo.postservice.service;

import com.travelo.postservice.dto.TopicDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TopicService {
    List<TopicDto> getTrendingTopics();
    
    Page<TopicDto> getTrendingTopics(Pageable pageable);
    
    List<TopicDto> searchTopics(String query);
    
    TopicDto createTopic(String name, String description);
    
    TopicDto associateTopicWithPost(String postId, String topicId);
    
    void updateTopicTrendScore(String topicId);
}

