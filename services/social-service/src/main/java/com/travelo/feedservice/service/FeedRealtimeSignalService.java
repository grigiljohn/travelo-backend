package com.travelo.feedservice.service;

import com.travelo.feedservice.client.dto.PostDto;
import com.travelo.feedservice.dto.FeedItem;
import com.travelo.feedservice.dto.FeedUserEventDto;

import java.util.List;
import java.util.UUID;

/**
 * Realtime user-signal + short-window fatigue service for feed ranking.
 */
public interface FeedRealtimeSignalService {

    void recordUserEvents(UUID userId, String surface, List<FeedUserEventDto> events);

    List<PostDto> applyOnlineSignals(UUID userId, String surface, List<PostDto> rankedPosts);

    double getOnlineSignalScore(UUID userId, String surface, String targetId);

    List<FeedItem> applySessionFatigue(UUID userId, String surface, List<FeedItem> feedItems);

    void recordServedItems(UUID userId, String surface, List<FeedItem> servedItems);
}

