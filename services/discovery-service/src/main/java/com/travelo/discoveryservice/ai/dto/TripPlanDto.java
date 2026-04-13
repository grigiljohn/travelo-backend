package com.travelo.discoveryservice.ai.dto;

import java.util.List;

public record TripPlanDto(String title, String summary, List<TripDayDto> days) {}
