package com.travelo.planservice.service;

import com.travelo.planservice.dto.AiGeneratePlanResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Heuristic “AI” suggestions (replace with LLM / external service).
 */
@Service
public class AiPlanService {

    public AiGeneratePlanResponse generate(String prompt) {
        String p = prompt == null ? "" : prompt.toLowerCase(Locale.ROOT).trim();
        String title = titleFrom(p);
        String location = locationFrom(p);
        String time = timeFrom(p);
        String desc = "Join for " + title + ". Open to travelers — bring good vibes and a camera.";
        List<String> tags = tagsFrom(p);
        return new AiGeneratePlanResponse(title, desc, tags, location, time);
    }

    public String enhanceDescription(String draft) {
        String d = draft == null ? "" : draft.trim();
        if (d.isEmpty()) {
            return "Meet friendly travelers, share the moment, and explore together.";
        }
        return d + " ✨ We’ll keep it relaxed, photo-friendly, and easy to join.";
    }

    private static String titleFrom(String p) {
        if (p.contains("sunset") && p.contains("hike")) {
            return "Sunset hike & golden hour photos";
        }
        if (p.contains("food") || p.contains("street")) {
            return "Street food crawl & local bites";
        }
        if (p.contains("coffee")) {
            return "Morning coffee & city walk";
        }
        if (p.contains("run") || p.contains("jog")) {
            return "Easy group run + cool-down stretch";
        }
        return "Travel meetup — " + (p.length() > 40 ? p.substring(0, 40) + "…" : (p.isEmpty() ? "explore together" : p));
    }

    private static String locationFrom(String p) {
        if (p.contains("kl") || p.contains("kuala")) {
            return "Kuala Lumpur city center";
        }
        if (p.contains("kochi")) {
            return "Kochi, Kerala";
        }
        if (p.contains("bali")) {
            return "Ubud, Bali";
        }
        return "Downtown — exact pin after RSVP";
    }

    private static String timeFrom(String p) {
        if (p.contains("sunrise")) {
            return "Tomorrow · 6:15 AM";
        }
        if (p.contains("sunset")) {
            return "Today · 6:30 PM";
        }
        if (p.contains("evening") || p.contains("night")) {
            return "Tonight · 7:30 PM";
        }
        return "This weekend · flexible start";
    }

    private static List<String> tagsFrom(String p) {
        List<String> out = new ArrayList<>();
        if (p.contains("hike") || p.contains("trail")) {
            out.add("Hiking");
        }
        if (p.contains("food")) {
            out.add("Food");
        }
        if (p.contains("photo")) {
            out.add("Photography");
        }
        if (p.contains("coffee")) {
            out.add("Coffee");
        }
        if (out.isEmpty()) {
            out.addAll(List.of("Meetup", "Travel", "Social"));
        }
        return out;
    }
}
