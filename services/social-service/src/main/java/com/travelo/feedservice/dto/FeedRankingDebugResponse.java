package com.travelo.feedservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class FeedRankingDebugResponse {

    @JsonProperty("surface")
    private String surface;

    @JsonProperty("mood")
    private String mood;

    @JsonProperty("requested_limit")
    private int requestedLimit;

    @JsonProperty("items")
    private List<FeedRankingDebugItem> items;

    @JsonProperty("config")
    private Map<String, Object> config;

    public String getSurface() {
        return surface;
    }

    public void setSurface(String surface) {
        this.surface = surface;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public int getRequestedLimit() {
        return requestedLimit;
    }

    public void setRequestedLimit(int requestedLimit) {
        this.requestedLimit = requestedLimit;
    }

    public List<FeedRankingDebugItem> getItems() {
        return items;
    }

    public void setItems(List<FeedRankingDebugItem> items) {
        this.items = items;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }
}

