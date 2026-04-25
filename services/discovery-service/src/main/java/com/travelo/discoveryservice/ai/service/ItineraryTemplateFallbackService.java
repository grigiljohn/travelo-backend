package com.travelo.discoveryservice.ai.service;

import com.travelo.discoveryservice.ai.dto.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic itinerary when OpenAI is disabled or fails. Mirrors the structure expected by the mobile app.
 */
@Service
public class ItineraryTemplateFallbackService {

    public BuildItineraryResponse build(BuildItineraryRequest request, String source) {
        String dest = request.destination() == null ? "your destination" : request.destination().trim();
        int days = request.durationDays() != null ? request.durationDays() : 5;
        days = Math.min(14, Math.max(1, days));

        String act = request.activities() == null || request.activities().isEmpty()
                ? "general sightseeing and local food"
                : String.join(", ", request.activities());
        String comp = request.companions() == null || request.companions().isEmpty()
                ? "travelers"
                : String.join(", ", request.companions());
        String budget = request.budget() == null || request.budget().isBlank() ? "flexible" : request.budget();
        String when = request.dateSummary() == null || request.dateSummary().isBlank()
                ? "your dates"
                : request.dateSummary();

        String title = "Your " + days + "-day plan — " + dest;
        String summary = String.format(
                "A %s-day suggested pace for %s, tailored to %s interests, %s style budget, with %s. "
                        + "Adjust times and add bookings as you confirm details.",
                days, dest, act, budget, comp);

        List<BuildItineraryDayPayload> dayList = new ArrayList<>();
        for (int d = 1; d <= days; d++) {
            dayList.add(buildDay(d, days, dest, act, when));
        }
        return new BuildItineraryResponse(title, summary, source, dayList);
    }

    private static BuildItineraryDayPayload buildDay(int dayNumber, int totalDays, String dest, String act, String when) {
        String theme = switch (dayNumber) {
            case 1 -> "Arrival & orientation";
            case 2 -> "Highlights & culture";
            case 3 -> "Nature or neighbourhoods";
            case 4 -> "Local flavours & free time";
            case 5 -> "Last look & departure";
            default -> "Exploring " + dest;
        };
        if (totalDays <= 3 && dayNumber == totalDays) {
            theme = "Highlights & departure";
        }
        if (dayNumber == totalDays && totalDays > 1) {
            theme = "Favourites & wrap-up";
        }

        var items = new ArrayList<BuildItineraryItemPayload>();
        items.add(new BuildItineraryItemPayload(
                "9:00 AM",
                "Morning",
                "Start the day in " + dest + " with a relaxed pace. Focus on: " + act + ".",
                "2h",
                "food",
                new TravelSegmentPayload("2 km", "10 min")
        ));
        items.add(new BuildItineraryItemPayload(
                "1:00 PM",
                "Afternoon",
                "Deeper exploration — museums, streets, or waterfront depending on the area. Calendar: " + when + ".",
                "3h",
                "walk",
                new TravelSegmentPayload("3 km", "12 min")
        ));
        String eveningDesc = (dayNumber == totalDays)
                ? "Packing, final meal, and transfer to your outbound connection if applicable."
                : "Dinner, sunset spot, or low-key night — match your energy.";
        items.add(new BuildItineraryItemPayload(
                "6:00 PM",
                "Evening",
                eveningDesc,
                "2h 30m",
                "nightlife",
                null
        ));

        return new BuildItineraryDayPayload(
                dayNumber,
                "Day " + dayNumber + " — " + theme,
                items
        );
    }
}
