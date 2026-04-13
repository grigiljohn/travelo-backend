package com.travelo.storyservice.service;

import com.travelo.storyservice.dto.*;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for story operations.
 */
public interface StoryService {
    
    /**
     * Create a new story.
     */
    StoryDto createStory(String userId, CreateStoryRequest request);
    
    /**
     * Get active stories for a user.
     */
    List<StoryDto> getUserStories(String userId, String viewerUserId);
    
    /**
     * Get active stories from followed users (for feed).
     */
    List<StoryDto> getFeedStories(List<String> followedUserIds, String viewerUserId);

    /**
     * Get active discover stories (for stories strip).
     */
    List<StoryDto> getDiscoverStories(String viewerUserId);
    
    /**
     * Get a single story by ID.
     */
    StoryDto getStory(UUID storyId, String viewerUserId);
    
    /**
     * Delete a story.
     */
    void deleteStory(UUID storyId, String userId);
    
    /**
     * Mark a story as viewed.
     */
    void markStoryAsViewed(UUID storyId, String userId);
    
    /**
     * Get story viewer list.
     */
    List<StoryViewerDto> getStoryViewers(UUID storyId, String ownerUserId);
    
    /**
     * Add reply to a story.
     */
    StoryReplyDto addReply(UUID storyId, String userId, CreateStoryReplyRequest request);
    
    /**
     * Get replies for a story.
     */
    List<StoryReplyDto> getStoryReplies(UUID storyId, String viewerUserId);
    
    /**
     * Create a story highlight.
     */
    StoryHighlightDto createHighlight(String userId, CreateStoryHighlightRequest request);
    
    /**
     * Get user's highlights.
     */
    List<StoryHighlightDto> getUserHighlights(String userId);
    
    /**
     * Get stories in a highlight.
     */
    List<StoryDto> getHighlightStories(UUID highlightId, String viewerUserId);
    
    /**
     * Add story to highlight.
     */
    void addStoryToHighlight(UUID storyId, UUID highlightId, String userId);
    
    /**
     * Remove story from highlight.
     */
    void removeStoryFromHighlight(UUID storyId, UUID highlightId, String userId);
}

