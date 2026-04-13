package com.travelo.discoveryservice.ai.dto;

import java.util.List;

/**
 * One day in a generated itinerary (hardcoded template until real LLM).
 */
public record TripDayDto(int dayNumber, String title, List<String> bullets) {}
