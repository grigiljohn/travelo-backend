package com.travelo.feedservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for feed with cursor-based pagination.
 */
public class FeedResponse {
    
    @JsonProperty("items")
    private List<FeedItem> items;
    
    @JsonProperty("next_cursor")
    private String nextCursor;
    
    @JsonProperty("has_more")
    private boolean hasMore;
    
    @JsonProperty("total_items")
    private Long totalItems;

    @JsonProperty("recommended_prefetch_limit")
    private Integer recommendedPrefetchLimit;

    public FeedResponse() {
    }

    public FeedResponse(List<FeedItem> items, String nextCursor, boolean hasMore, Long totalItems,
                        Integer recommendedPrefetchLimit) {
        this.items = items;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
        this.totalItems = totalItems;
        this.recommendedPrefetchLimit = recommendedPrefetchLimit;
    }

    public List<FeedItem> getItems() {
        return items;
    }

    public void setItems(List<FeedItem> items) {
        this.items = items;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public Long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Long totalItems) {
        this.totalItems = totalItems;
    }

    public Integer getRecommendedPrefetchLimit() {
        return recommendedPrefetchLimit;
    }

    public void setRecommendedPrefetchLimit(Integer recommendedPrefetchLimit) {
        this.recommendedPrefetchLimit = recommendedPrefetchLimit;
    }
}

