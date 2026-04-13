package com.travelo.planservice.service;

import com.travelo.planservice.dto.RichPlanResponse;
import com.travelo.planservice.dto.UnifiedSearchHit;
import com.travelo.planservice.dto.UnifiedSearchResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * In-memory unified search (users, curated locations, trips, live plans).
 * Locations are curated until Google Places is wired; plans come from {@link RichPlanService}.
 */
@Service
public class UnifiedSearchService {

    private static final int CAP = 12;

    private final RichPlanService richPlanService;

    public UnifiedSearchService(RichPlanService richPlanService) {
        this.richPlanService = richPlanService;
    }

    public UnifiedSearchResponse search(String rawQuery, String category) {
        String q = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);
        if (q.length() < 2) {
            return new UnifiedSearchResponse(List.of(), List.of(), List.of(), List.of());
        }
        boolean all = category == null || category.isBlank() || "all".equalsIgnoreCase(category.trim());
        String cat = category == null ? "all" : category.trim().toLowerCase(Locale.ROOT);

        List<UnifiedSearchHit> users = (all || "users".equals(cat) || "user".equals(cat)) ? matchUsers(q) : List.of();
        List<UnifiedSearchHit> locations = (all || "locations".equals(cat) || "location".equals(cat) || "places".equals(cat))
                ? matchLocations(q) : List.of();
        List<UnifiedSearchHit> trips = (all || "trips".equals(cat) || "trip".equals(cat)) ? matchTrips(q) : List.of();
        List<UnifiedSearchHit> plans = (all || "plans".equals(cat) || "plan".equals(cat)) ? matchPlans(q) : List.of();

        return new UnifiedSearchResponse(users, locations, trips, plans);
    }

    private static List<UnifiedSearchHit> matchUsers(String q) {
        List<UnifiedSearchHit> out = new ArrayList<>();
        for (UnifiedSearchHit h : SEED_USERS) {
            if (contains(h.title(), q) || contains(h.subtitle(), q) || contains((String) h.metadata().getOrDefault("username", ""), q)) {
                out.add(h);
            }
            if (out.size() >= CAP) {
                break;
            }
        }
        return out;
    }

    private static List<UnifiedSearchHit> matchLocations(String q) {
        List<UnifiedSearchHit> out = new ArrayList<>();
        for (UnifiedSearchHit h : SEED_LOCATIONS) {
            if (contains(h.title(), q) || contains(h.subtitle(), q)
                    || contains(String.valueOf(h.metadata().getOrDefault("country", "")), q)) {
                out.add(h);
            }
            if (out.size() >= CAP) {
                break;
            }
        }
        return out;
    }

    private static List<UnifiedSearchHit> matchTrips(String q) {
        List<UnifiedSearchHit> out = new ArrayList<>();
        for (UnifiedSearchHit h : SEED_TRIPS) {
            if (contains(h.title(), q) || contains(h.subtitle(), q)) {
                out.add(h);
            }
            if (out.size() >= CAP) {
                break;
            }
        }
        return out;
    }

    private List<UnifiedSearchHit> matchPlans(String q) {
        List<UnifiedSearchHit> out = new ArrayList<>();
        for (RichPlanResponse r : richPlanService.feed()) {
            if (contains(r.title(), q) || contains(r.locationName(), q) || contains(r.description(), q)) {
                out.add(fromPlan(r));
            }
            if (out.size() >= CAP) {
                break;
            }
        }
        return out;
    }

    private static UnifiedSearchHit fromPlan(RichPlanResponse r) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("planId", r.id());
        meta.put("locationName", r.locationName());
        meta.put("timeLabel", r.timeLabel());
        meta.put("hostName", r.hostName());
        meta.put("hostAvatarUrl", r.hostAvatarUrl() != null ? r.hostAvatarUrl() : "");
        meta.put("maxPeople", r.maxPeople());
        meta.put("description", r.description() != null ? r.description() : "");
        meta.put("badge", r.badge());
        meta.put("mediaType", r.mediaType());
        String subtitle = (r.timeLabel() != null ? r.timeLabel() : "") + " · " + (r.locationName() != null ? r.locationName() : "");
        return new UnifiedSearchHit(r.id(), "plan", r.title(), subtitle.trim(), r.mediaUrl(), meta);
    }

    private static boolean contains(String hay, String needle) {
        if (hay == null || needle.isEmpty()) {
            return false;
        }
        return hay.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static final List<UnifiedSearchHit> SEED_USERS = List.of(
            user("u_arjun", "Arjun", "@arjun_wanders", "https://picsum.photos/seed/travelo-user-arjun/200/200", "Kuala Lumpur"),
            user("u_maya", "Maya Chen", "@maya.c", "https://picsum.photos/seed/travelo-user-maya/200/200", "Singapore"),
            user("u_jon", "Jon Rivera", "@jonontheroad", "https://picsum.photos/seed/travelo-user-jon/200/200", "Barcelona"),
            user("u_sofia", "Sofia", "@sofia.lens", "https://picsum.photos/seed/travelo-user-sofia/200/200", "Lisbon"),
            user("u_ken", "Ken Tanaka", "@ken.trip", "https://picsum.photos/seed/travelo-user-ken/200/200", "Tokyo")
    );

    private static UnifiedSearchHit user(String id, String name, String username, String avatar, String city) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("username", username.replace("@", ""));
        meta.put("city", city);
        return new UnifiedSearchHit(id, "user", name, username, avatar, meta);
    }

    private static final List<UnifiedSearchHit> SEED_LOCATIONS = List.of(
            loc("loc_bali", "Bali", "Indonesia · beaches & temples", -8.409518, 115.188919),
            loc("loc_kl", "Kuala Lumpur", "Malaysia · city & food", 3.139003, 101.686855),
            loc("loc_tokyo", "Tokyo", "Japan · culture & food", 35.6762, 139.6503),
            loc("loc_paris", "Paris", "France · art & cafés", 48.8566, 2.3522),
            loc("loc_lisbon", "Lisbon", "Portugal · tiles & surf", 38.7223, -9.1393),
            loc("loc_dubai", "Dubai", "UAE · skyline & desert", 25.2048, 55.2708),
            loc("loc_kyoto", "Kyoto", "Japan · temples", 35.0116, 135.7681),
            loc("loc_cape", "Cape Town", "South Africa · coast", -33.9249, 18.4241)
    );

    private static UnifiedSearchHit loc(String id, String title, String subtitle, double lat, double lng) {
        Map<String, Object> meta = new LinkedHashMap<>();
        String[] parts = subtitle.split(" · ", 2);
        meta.put("country", parts.length > 0 ? parts[0] : "");
        meta.put("tagline", parts.length > 1 ? parts[1] : subtitle);
        meta.put("lat", lat);
        meta.put("lng", lng);
        return new UnifiedSearchHit(id, "location", title, subtitle,
                "https://picsum.photos/seed/travelo-loc-" + id + "/400/300", meta);
    }

    private static final List<UnifiedSearchHit> SEED_TRIPS = List.of(
            trip("trip_bali", "Bali reset week", "7 days · beach + rice terraces"),
            trip("trip_eurail", "Euro rail hop", "14 days · 5 cities"),
            trip("trip_kyoto", "Kyoto slow days", "5 days · temples & tea"),
            trip("trip_alps", "Swiss Alps hiking", "6 days · guided peaks"),
            trip("trip_kl_food", "KL street food crawl", "3 days · night markets"),
            trip("trip_tokyo", "Tokyo nights", "4 days · neon & izakaya")
    );

    private static UnifiedSearchHit trip(String id, String title, String subtitle) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("durationHint", subtitle);
        return new UnifiedSearchHit(id, "trip", title, subtitle,
                "https://picsum.photos/seed/travelo-trip-" + id + "/400/300", meta);
    }
}
