package com.travelo.feedservice.service;

import com.travelo.feedservice.dto.FeedUserEventDto;

import java.util.List;
import java.util.UUID;

/**
 * Publishes feed-side user events (impressions, clicks, dwell) for analytics / ML training.
 */
public interface FeedInteractionEventPublisher {

    void publish(UUID userId, String surface, List<FeedUserEventDto> events);
}
