package com.travelo.storyservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelo.storyservice.client.MediaServiceClient;
import com.travelo.storyservice.dto.*;
import com.travelo.storyservice.entity.Story;
import com.travelo.storyservice.entity.StoryHighlight;
import com.travelo.storyservice.entity.StoryReply;
import com.travelo.storyservice.entity.StoryView;
import com.travelo.storyservice.exception.StoryNotFoundException;
import com.travelo.storyservice.repository.*;
import com.travelo.storyservice.service.StoryService;
import com.travelo.storyservice.service.StoryTTLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(transactionManager = "storyTransactionManager")
public class StoryServiceImpl implements StoryService {

    private static final Logger logger = LoggerFactory.getLogger(StoryServiceImpl.class);
    private static final long STORY_TTL_SECONDS = 24 * 60 * 60; // 24 hours
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final StoryRepository storyRepository;
    private final StoryViewRepository storyViewRepository;
    private final StoryReplyRepository storyReplyRepository;
    private final StoryHighlightRepository storyHighlightRepository;
    private final StoryTTLService storyTTLService;
    private final MediaServiceClient mediaServiceClient;

    public StoryServiceImpl(
            StoryRepository storyRepository,
            StoryViewRepository storyViewRepository,
            StoryReplyRepository storyReplyRepository,
            StoryHighlightRepository storyHighlightRepository,
            StoryTTLService storyTTLService,
            MediaServiceClient mediaServiceClient) {
        this.storyRepository = storyRepository;
        this.storyViewRepository = storyViewRepository;
        this.storyReplyRepository = storyReplyRepository;
        this.storyHighlightRepository = storyHighlightRepository;
        this.storyTTLService = storyTTLService;
        this.mediaServiceClient = mediaServiceClient;
    }

    @Override
    public StoryDto createStory(String userId, CreateStoryRequest request) {
        logger.info("Creating story for user {}", userId);

        // Fetch media details from media-service
        String mediaUrl = request.getMediaUrl();
        String thumbnailUrl = null;
        Story.MediaType mediaType = request.getMediaType() != null
                ? request.getMediaType()
                : Story.MediaType.IMAGE;

        if ((mediaUrl == null || mediaUrl.isBlank()) && request.getMediaId() != null) {
            // TODO: Fetch media type from media-service
            mediaUrl = mediaServiceClient.getDownloadUrl(request.getMediaId());
        }

        Story story = new Story(
                userId,
                request.getMediaId(),
                mediaUrl,
                thumbnailUrl,
                mediaType,
                request.getCaption()
        );
        story.setUserName(request.getUserName());
        story.setUserAvatar(request.getUserAvatar());
        story.setVideoUrl(request.getVideoUrl());
        story.setImageUrlsJson(request.getImageUrlsJson());
        story.setLocation(request.getLocation());
        story.setBestTime(request.getBestTime());
        story.setInsight(request.getInsight());
        story.setStoryType(request.getStoryType());
        story.setMusicTrack(request.getMusicTrack());

        story = storyRepository.save(story);

        // Set TTL in Redis
        storyTTLService.setStoryTTL(story.getId(), STORY_TTL_SECONDS);

        logger.info("Created story {} for user {}", story.getId(), userId);
        return toDto(story, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryDto> getUserStories(String userId, String viewerUserId) {
        logger.debug("Getting stories for user {}", userId);
        OffsetDateTime now = OffsetDateTime.now();
        List<Story> stories = storyRepository.findActiveStoriesByUserId(userId, now);
        return stories.stream()
                .map(story -> toDto(story, viewerUserId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryDto> getFeedStories(List<String> followedUserIds, String viewerUserId) {
        logger.debug("Getting feed stories for {} users", followedUserIds.size());
        OffsetDateTime now = OffsetDateTime.now();
        List<Story> stories = storyRepository.findActiveStoriesByUserIds(followedUserIds, now);
        return stories.stream()
                .map(story -> toDto(story, viewerUserId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryDto> getDiscoverStories(String viewerUserId) {
        logger.debug("Getting discover stories");
        OffsetDateTime now = OffsetDateTime.now();
        List<Story> stories = storyRepository.findDiscoverStories(now);
        return stories.stream()
                .map(story -> toDto(story, viewerUserId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StoryDto getStory(UUID storyId, String viewerUserId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new StoryNotFoundException(storyId));
        
        // Check if expired
        if (story.getExpiresAt().isBefore(OffsetDateTime.now()) && !story.getIsHighlight()) {
            throw new StoryNotFoundException(storyId);
        }

        return toDto(story, viewerUserId);
    }

    @Override
    public void deleteStory(UUID storyId, String userId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new StoryNotFoundException(storyId));
        
        if (!story.getUserId().equals(userId)) {
            throw new IllegalArgumentException("User does not own this story");
        }

        storyRepository.delete(story);
        storyTTLService.removeStoryTTL(storyId);
        logger.info("Deleted story {}", storyId);
    }

    @Override
    public void markStoryAsViewed(UUID storyId, String userId) {
        // Check if already viewed
        Optional<StoryView> existingView = storyViewRepository.findByStoryIdAndUserId(storyId, userId);
        if (existingView.isPresent()) {
            logger.debug("Story {} already viewed by user {}", storyId, userId);
            return;
        }

        // Check if story exists and is active
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new StoryNotFoundException(storyId));

        if (story.getExpiresAt().isBefore(OffsetDateTime.now()) && !story.getIsHighlight()) {
            logger.debug("Story {} has expired", storyId);
            return;
        }

        // Create view
        StoryView view = new StoryView(storyId, userId);
        storyViewRepository.save(view);

        // Update view count
        story.setViewCount(story.getViewCount() + 1);
        storyRepository.save(story);

        logger.debug("Marked story {} as viewed by user {}", storyId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryViewerDto> getStoryViewers(UUID storyId, String ownerUserId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new StoryNotFoundException(storyId));

        if (!story.getUserId().equals(ownerUserId)) {
            throw new IllegalArgumentException("User does not own this story");
        }

        List<StoryView> views = storyViewRepository.findByStoryId(storyId);
        return views.stream()
                .map(StoryViewerDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public StoryReplyDto addReply(UUID storyId, String userId, CreateStoryReplyRequest request) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new StoryNotFoundException(storyId));

        if (story.getExpiresAt().isBefore(OffsetDateTime.now()) && !story.getIsHighlight()) {
            throw new StoryNotFoundException(storyId);
        }

        StoryReply reply = new StoryReply(storyId, userId, request.getReplyText());
        reply = storyReplyRepository.save(reply);

        // Update reply count
        story.setReplyCount(story.getReplyCount() + 1);
        storyRepository.save(story);

        logger.info("Added reply {} to story {}", reply.getId(), storyId);
        return StoryReplyDto.fromEntity(reply);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryReplyDto> getStoryReplies(UUID storyId, String viewerUserId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new StoryNotFoundException(storyId));

        List<StoryReply> replies = storyReplyRepository.findByStoryId(storyId);
        return replies.stream()
                .map(StoryReplyDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public StoryHighlightDto createHighlight(String userId, CreateStoryHighlightRequest request) {
        StoryHighlight highlight = new StoryHighlight(userId, request.getTitle(), request.getCoverImageUrl());
        StoryHighlight savedHighlight = storyHighlightRepository.save(highlight);
        logger.info("Created highlight {} for user {}", savedHighlight.getId(), userId);
        return StoryHighlightDto.fromEntity(savedHighlight);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryHighlightDto> getUserHighlights(String userId) {
        List<StoryHighlight> highlights = storyHighlightRepository.findByUserId(userId);
        return highlights.stream()
                .map(StoryHighlightDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryDto> getHighlightStories(UUID highlightId, String viewerUserId) {
        StoryHighlight highlight = storyHighlightRepository.findById(highlightId)
                .orElseThrow(() -> new IllegalArgumentException("Highlight not found"));

        List<Story> stories = storyRepository.findStoriesByHighlightId(highlightId);
        return stories.stream()
                .map(story -> toDto(story, viewerUserId))
                .collect(Collectors.toList());
    }

    @Override
    public void addStoryToHighlight(UUID storyId, UUID highlightId, String userId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new StoryNotFoundException(storyId));

        if (!story.getUserId().equals(userId)) {
            throw new IllegalArgumentException("User does not own this story");
        }

        StoryHighlight highlight = storyHighlightRepository.findById(highlightId)
                .orElseThrow(() -> new IllegalArgumentException("Highlight not found"));

        if (!highlight.getUserId().equals(userId)) {
            throw new IllegalArgumentException("User does not own this highlight");
        }

        story.setHighlightId(highlightId);
        story.setIsHighlight(true);
        storyRepository.save(story);

        // Update highlight story count
        StoryHighlight savedHighlight = storyHighlightRepository.findById(highlightId).orElse(highlight);
        savedHighlight.setStoryCount(savedHighlight.getStoryCount() + 1);
        storyHighlightRepository.save(savedHighlight);

        // Remove TTL (highlights don't expire)
        storyTTLService.removeStoryTTL(storyId);

        logger.info("Added story {} to highlight {}", storyId, highlightId);
    }

    @Override
    public void removeStoryFromHighlight(UUID storyId, UUID highlightId, String userId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new StoryNotFoundException(storyId));

        if (!story.getUserId().equals(userId)) {
            throw new IllegalArgumentException("User does not own this story");
        }

        if (!highlightId.equals(story.getHighlightId())) {
            throw new IllegalArgumentException("Story is not in this highlight");
        }

        story.setHighlightId(null);
        story.setIsHighlight(false);
        storyRepository.save(story);

        StoryHighlight highlight = storyHighlightRepository.findById(highlightId)
                .orElseThrow(() -> new IllegalArgumentException("Highlight not found"));

        highlight.setStoryCount(Math.max(0, highlight.getStoryCount() - 1));
        storyHighlightRepository.save(highlight);

        logger.info("Removed story {} from highlight {}", storyId, highlightId);
    }

    private StoryDto toDto(Story story, String viewerUserId) {
        if ((story.getImageUrlsJson() == null || story.getImageUrlsJson().isBlank())
                && story.getMediaUrl() != null && !story.getMediaUrl().isBlank()) {
            try {
                story.setImageUrlsJson(OBJECT_MAPPER.writeValueAsString(List.of(story.getMediaUrl())));
            } catch (Exception ignored) {
                // Keep as null if serialization fails
            }
        }
        StoryDto dto = StoryDto.fromEntity(story);
        
        // Check if viewed by viewer
        if (viewerUserId != null) {
            boolean isViewed = storyViewRepository.findByStoryIdAndUserId(story.getId(), viewerUserId).isPresent();
            dto.setIsViewed(isViewed);
        }
        
        // Get remaining TTL
        long remainingTtl = storyTTLService.getRemainingTTL(story.getId());
        if (remainingTtl > 0) {
            dto.setRemainingTtlSeconds(remainingTtl);
        } else if (!story.getIsHighlight() && story.getExpiresAt() != null) {
            // Calculate from expires_at if Redis TTL not available
            OffsetDateTime now = OffsetDateTime.now();
            if (story.getExpiresAt().isAfter(now)) {
                dto.setRemainingTtlSeconds(java.time.Duration.between(now, story.getExpiresAt()).getSeconds());
            } else {
                dto.setRemainingTtlSeconds(0L);
            }
        }
        
        return dto;
    }
}

