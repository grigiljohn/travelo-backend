package com.travelo.postservice.service.impl;

import com.travelo.postservice.dto.GenerateTimelineRequest;
import com.travelo.postservice.dto.StoryTimelineDto;
import com.travelo.postservice.entity.StoryTimeline;
import com.travelo.postservice.repository.StoryTimelineRepository;
import com.travelo.postservice.service.StoryTimelineService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StoryTimelineServiceImpl implements StoryTimelineService {
    private static final Logger logger = LoggerFactory.getLogger(StoryTimelineServiceImpl.class);
    
    private final StoryTimelineRepository storyTimelineRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.ai-orchestrator.base-url:http://127.0.0.1:8099}")
    private String aiOrchestratorBaseUrl;
    
    public StoryTimelineServiceImpl(StoryTimelineRepository storyTimelineRepository) {
        this.storyTimelineRepository = storyTimelineRepository;
    }
    
    @Override
    @Transactional
    @SuppressWarnings("DataFlowIssue")
    public StoryTimelineDto generateTimeline(String userId, GenerateTimelineRequest request) {
        List<String> mediaOrder = new ArrayList<>(request.mediaFiles());
        Map<String, Integer> durations = new HashMap<>();
        List<Map<String, Object>> transitions = new ArrayList<>();

        // Try to delegate to AI orchestrator first; fall back to simple heuristic on failure.
        try {
            String url = aiOrchestratorBaseUrl + "/internal/auto-cut/timeline";
            Map<String, Object> body = new HashMap<>();
            body.put("mediaIds", mediaOrder);
            body.put("templateId", request.templateId());
            body.put("targetDurationMs", null);

            @SuppressWarnings({"unchecked", "DataFlowIssue"})
            ResponseEntity<Map<String, Object>> response =
                    (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>)
                            restTemplate.exchange(
                                    url,
                                    HttpMethod.POST,
                                    new HttpEntity<>(body),
                                    Map.class
                            );

            Map<String, Object> resp = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && resp != null) {
                @SuppressWarnings("unchecked")
                List<String> ordered = (List<String>) resp.getOrDefault("mediaOrder", mediaOrder);
                @SuppressWarnings("unchecked")
                Map<String, Integer> respDurations = (Map<String, Integer>) resp.get("durationsMs");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> respTransitions =
                        (List<Map<String, Object>>) resp.get("transitions");

                if (respDurations != null && !respDurations.isEmpty()) {
                    durations.putAll(respDurations);
                }
                if (respTransitions != null) {
                    transitions.addAll(respTransitions);
                }
                mediaOrder = ordered;
                logger.info("Story timeline generated via AI orchestrator for user {}", userId);
            } else {
                logger.warn("AI orchestrator returned non-2xx response, falling back to simple timeline");
            }
        } catch (Exception ex) {
            logger.warn("Failed to call AI orchestrator for timeline generation, using fallback: {}", ex.getMessage());
        }

        // Fallback: simple equal-duration timeline if orchestrator did not populate data.
        if (durations.isEmpty()) {
            for (String mediaId : mediaOrder) {
                durations.put(mediaId, 3000);
            }
        }
        if (transitions.isEmpty() && mediaOrder.size() > 1) {
            for (int i = 0; i < mediaOrder.size() - 1; i++) {
                Map<String, Object> transition = new HashMap<>();
                transition.put("from", i);
                transition.put("to", i + 1);
                transition.put("type", "fade");
                transition.put("duration", 500);
                transitions.add(transition);
            }
        }

        StoryTimeline timeline = StoryTimeline.builder()
            .userId(userId)
            .mediaOrder(mediaOrder)
            .durations(durations)
            .transitions(transitions)
            .templateId(request.templateId())
            .build();
        
        timeline = storyTimelineRepository.save(timeline);
        return toDto(timeline);
    }
    
    @Override
    public List<StoryTimelineDto> getUserTimelines(String userId) {
        return storyTimelineRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    private StoryTimelineDto toDto(StoryTimeline timeline) {
        return new StoryTimelineDto(
            timeline.getId(),
            timeline.getUserId(),
            timeline.getMediaOrder(),
            timeline.getDurations(),
            timeline.getTransitions(),
            timeline.getTextOverlays(),
            timeline.getTemplateId(),
            timeline.getCreatedAt()
        );
    }
}

