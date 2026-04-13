package com.travelo.discoveryservice.ai.service;

import com.travelo.discoveryservice.ai.dto.AiTripChatRequest;
import com.travelo.discoveryservice.ai.dto.AiTripChatResponse;
import com.travelo.discoveryservice.ai.dto.TripDayDto;
import com.travelo.discoveryservice.ai.dto.TripPlanDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collects destination, duration, budget, and interests through short prompts, then returns a
 * hardcoded itinerary template (placeholder until a real model is wired).
 */
@Service
public class AiTripOrchestratorService {

    private static final Pattern DURATION = Pattern.compile(
            "(\\d{1,2})\\s*(?:day|days|night|nights)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DESTINATION_IN = Pattern.compile(
            "\\b(?:in|to|for)\\s+([A-Za-z][A-Za-z\\s'.-]{1,48})\\b");
    private static final Pattern DESTINATION_PLAN = Pattern.compile(
            "\\bplan\\s+(?:in|for)\\s+([A-Za-z][A-Za-z\\s'.-]{1,48})\\b",
            Pattern.CASE_INSENSITIVE);

    public AiTripChatResponse chat(AiTripChatRequest request) {
        String last = request.lastUserMessage() == null ? "" : request.lastUserMessage().trim();
        String dest = blankToNull(request.destination());
        Integer days = request.durationDays();
        String budget = normalizeBudget(request.budgetStyle());
        String interests = blankToNull(request.interests());

        SlotState slots = new SlotState(dest, days, budget, interests);
        applyHeuristics(last, slots);

        if (slots.destination == null) {
            return respond(
                    "Where would you like to go? (A city or country is perfect — e.g. Kyoto, Bali, Portugal.)",
                    slots,
                    false,
                    null);
        }
        if (slots.durationDays == null) {
            return respond(
                    "How many days will you be away? (A number is enough — e.g. 4.)",
                    slots,
                    false,
                    null);
        }
        if (slots.budget == null) {
            return respond(
                    "What budget style fits best: **budget-friendly**, **moderate**, or **luxury**?",
                    slots,
                    false,
                    null);
        }
        if (slots.interests == null || slots.interests.isBlank()) {
            return respond(
                    "Any must-haves? Short phrases work — e.g. temples, food tours, beaches, family-friendly.",
                    slots,
                    false,
                    null);
        }

        TripPlanDto plan = buildHardcodedPlan(slots);
        String narrative = buildNarrative(slots, plan);
        return new AiTripChatResponse(
                narrative,
                slots.destination,
                slots.durationDays,
                slots.budget,
                slots.interests,
                true,
                plan);
    }

    private static AiTripChatResponse respond(
            String assistantMessage, SlotState slots, boolean complete, TripPlanDto plan) {
        return new AiTripChatResponse(
                assistantMessage,
                slots.destination,
                slots.durationDays,
                slots.budget,
                slots.interests,
                complete,
                plan);
    }

    private void applyHeuristics(String last, SlotState slots) {
        if (last.isBlank()) {
            return;
        }
        String lower = last.toLowerCase(Locale.ROOT);

        Matcher dm = DURATION.matcher(lower);
        if (dm.find() && slots.durationDays == null) {
            slots.durationDays = Integer.parseInt(dm.group(1));
        }
        if (slots.durationDays == null) {
            Matcher solo = Pattern.compile("^\\s*(\\d{1,2})\\s*$").matcher(last);
            if (solo.matches()) {
                slots.durationDays = Integer.parseInt(solo.group(1));
            }
        }

        Matcher im = DESTINATION_IN.matcher(last);
        if (im.find() && slots.destination == null) {
            slots.destination = cleanDestination(im.group(1));
        }
        Matcher pm = DESTINATION_PLAN.matcher(last);
        if (pm.find() && slots.destination == null) {
            slots.destination = cleanDestination(pm.group(1));
        }

        if (slots.budget == null) {
            if (lower.contains("luxury") || lower.contains("splurge") || lower.contains("high end")) {
                slots.budget = "luxury";
            } else if (lower.contains("budget") || lower.contains("cheap") || lower.contains("low cost")
                    || lower.contains("affordable")) {
                slots.budget = "budget";
            } else if (lower.contains("moderate") || lower.contains("medium")) {
                slots.budget = "medium";
            }
        }

        if (slots.interests == null && looksLikeInterestsList(last)) {
            slots.interests = last.trim();
        }

        if (slots.destination == null
                && !last.contains(",")
                && last.length() >= 2
                && last.length() <= 48
                && last.split("\\s+").length <= 5
                && !last.matches(".*\\d.*")
                && !lower.contains("day")
                && !lower.contains("budget")
                && !lower.contains("luxury")
                && !lower.contains("moderate")) {
            slots.destination = cleanDestination(last);
        }
    }

    private static boolean looksLikeInterestsList(String last) {
        String l = last.toLowerCase(Locale.ROOT);
        return last.contains(",")
                || l.contains("tour")
                || l.contains("beach")
                || l.contains("food")
                || l.contains("temple")
                || l.contains("museum")
                || l.contains("family")
                || l.contains("nightlife")
                || l.contains("hike");
    }

    private static String cleanDestination(String raw) {
        String t = raw.trim();
        if (t.endsWith(".") || t.endsWith(",")) {
            t = t.substring(0, t.length() - 1).trim();
        }
        return t.isEmpty() ? null : t;
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalizeBudget(String b) {
        if (b == null) {
            return null;
        }
        String x = b.trim().toLowerCase(Locale.ROOT);
        if (x.isEmpty()) {
            return null;
        }
        if (x.contains("luxury") || x.contains("splurge")) {
            return "luxury";
        }
        if (x.contains("budget") || x.contains("cheap") || x.contains("low")) {
            return "budget";
        }
        if (x.contains("moderate") || x.contains("medium") || x.contains("mid")) {
            return "medium";
        }
        return null;
    }

    private TripPlanDto buildHardcodedPlan(SlotState s) {
        int n = Math.min(Math.max(s.durationDays, 1), 10);
        List<TripDayDto> days = new ArrayList<>();
        for (int d = 1; d <= n; d++) {
            days.add(switch (d) {
                case 1 -> new TripDayDto(
                        d,
                        "Arrival & easy exploring",
                        List.of(
                                "Check in, unpack, light walk nearby.",
                                "Casual dinner — local staple dish.",
                                "Early night to beat jet lag."));
                case 2 -> new TripDayDto(
                        d,
                        "Signature sights",
                        List.of(
                                "Morning: iconic viewpoint or old town.",
                                "Lunch: market or food hall (" + s.interests + ").",
                                "Afternoon: museum or cultural stop."));
                default -> {
                    if (d == n) {
                        yield new TripDayDto(
                                d,
                                "Departure day",
                                List.of(
                                        "Brunch and last-minute souvenirs.",
                                        "Buffer time to the airport/station.",
                                        "Save one snack for the journey home."));
                    }
                    yield new TripDayDto(
                            d,
                            "Flexible discovery",
                            List.of(
                                    "Pick one highlight from your interests: " + s.interests + ".",
                                    "Leave 2h unscheduled for spontaneity.",
                                    "Sunset viewpoint or waterfront stroll."));
                }
            });
        }
        String title = n + "-day " + s.destination + " outline";
        String summary = "A balanced " + s.budget + "-style route with room for "
                + s.interests
                + ". Distances and venues are illustrative — swap blocks to match real openings.";
        return new TripPlanDto(title, summary, days);
    }

    private String buildNarrative(SlotState s, TripPlanDto plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("Here’s your **").append(plan.title()).append("**.\n\n");
        sb.append(plan.summary()).append("\n\n");
        for (TripDayDto day : plan.days()) {
            sb.append("**Day ").append(day.dayNumber()).append(" — ").append(day.title()).append("**\n");
            for (String b : day.bullets()) {
                sb.append("• ").append(b).append("\n");
            }
            sb.append("\n");
        }
        sb.append("_This itinerary is sample data from Travelo Discovery AI — plug in real bookings when ready._");
        return sb.toString();
    }

    private static final class SlotState {
        String destination;
        Integer durationDays;
        String budget;
        String interests;

        SlotState(String destination, Integer durationDays, String budget, String interests) {
            this.destination = destination;
            this.durationDays = durationDays;
            this.budget = budget;
            this.interests = interests;
        }
    }
}
