package com.travelo.feedservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Batch of feed interaction events from the mobile client.
 */
public class FeedUserEventsRequest {

    @NotBlank
    @JsonProperty("surface")
    private String surface;

    @NotNull
    @Valid
    @Size(max = 100)
    @JsonProperty("events")
    private List<FeedUserEventDto> events;

    public String getSurface() {
        return surface;
    }

    public void setSurface(String surface) {
        this.surface = surface;
    }

    public List<FeedUserEventDto> getEvents() {
        return events;
    }

    public void setEvents(List<FeedUserEventDto> events) {
        this.events = events;
    }
}
