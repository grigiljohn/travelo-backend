package com.travelo.postservice.service;

import com.travelo.postservice.dto.GenerateTimelineRequest;
import com.travelo.postservice.dto.StoryTimelineDto;

import java.util.List;

public interface StoryTimelineService {
    StoryTimelineDto generateTimeline(String userId, GenerateTimelineRequest request);
    
    List<StoryTimelineDto> getUserTimelines(String userId);
}

