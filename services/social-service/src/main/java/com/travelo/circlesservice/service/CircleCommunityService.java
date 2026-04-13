package com.travelo.circlesservice.service;

import com.travelo.circlesservice.dto.CommunityResponse;
import com.travelo.circlesservice.dto.CreateCommunityRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CircleCommunityService {

    private final Map<String, Community> communities = new ConcurrentHashMap<>();

    @PostConstruct
    public void seed() {
        put(community(
                "g1",
                "Munnar Weekend Travelers",
                "Weekend escapes, tea estates, and sunrise viewpoints.",
                List.of("Hiking", "Photography", "Weekends"),
                "public",
                "Kuala Lumpur",
                setOf("alice", "bob"),
                "system",
                "2m ago"
        ));
        put(community(
                "g2",
                "Backpackers in KL",
                "Cheap eats, hostels, and night-market crawls.",
                List.of("Food", "Budget", "Social"),
                "public",
                "Kuala Lumpur",
                setOf("carol"),
                "system",
                "15m ago"
        ));
        put(community(
                "g3",
                "Private Photo Walk",
                "Invite-only · street photography sessions.",
                List.of("Photo", "City"),
                "private",
                "Kuala Lumpur",
                setOf("diana"),
                "diana",
                "1h ago"
        ));
    }

    private static Set<String> setOf(String... xs) {
        LinkedHashSet<String> s = new LinkedHashSet<>();
        for (String x : xs) {
            s.add(x);
        }
        return s;
    }

    private static Community community(
            String id,
            String name,
            String description,
            List<String> tags,
            String visibility,
            String city,
            Set<String> memberIds,
            String ownerId,
            String lastActivity
    ) {
        Community c = new Community();
        c.id = id;
        c.name = name;
        c.description = description;
        c.tags = new ArrayList<>(tags);
        c.visibility = normalizeVis(visibility);
        c.city = city;
        c.memberIds = new LinkedHashSet<>(memberIds);
        c.ownerId = ownerId;
        c.lastActivity = lastActivity;
        return c;
    }

    private void put(Community c) {
        communities.put(c.id, c);
    }

    private static String normalizeVis(String v) {
        if (v == null) {
            return "public";
        }
        String x = v.trim().toLowerCase(Locale.ROOT);
        return "private".equals(x) ? "private" : "public";
    }

    public List<CommunityResponse> list(String city, String currentUserId) {
        String user = blankToGuest(currentUserId);
        return communities.values().stream()
                .filter(c -> visibleTo(c, user))
                .sorted(Comparator.comparing((Community c) -> !c.memberIds.contains(user))
                        .thenComparing(c -> c.name))
                .map(c -> toDto(c, user))
                .collect(Collectors.toList());
    }

    private boolean visibleTo(Community c, String user) {
        if ("public".equals(c.visibility)) {
            return true;
        }
        return c.ownerId.equals(user) || c.memberIds.contains(user);
    }

    public CommunityResponse create(CreateCommunityRequest req, String ownerId) {
        String user = blankToGuest(ownerId);
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name required");
        }
        String id = "c_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Community c = new Community();
        c.id = id;
        c.name = req.getName().trim();
        c.description = req.getDescription() == null ? "" : req.getDescription().trim();
        c.tags = req.getTags() == null ? new ArrayList<>() : new ArrayList<>(req.getTags());
        c.visibility = normalizeVis(req.getVisibility());
        c.city = req.getCity() == null ? "" : req.getCity().trim();
        c.memberIds = new LinkedHashSet<>();
        c.memberIds.add(user);
        c.ownerId = user;
        c.lastActivity = "Just now";
        communities.put(id, c);
        return toDto(c, user);
    }

    public CommunityResponse join(String id, String userId) {
        String user = blankToGuest(userId);
        Community c = communities.get(id);
        if (c == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "community not found");
        }
        if (!visibleTo(c, user)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "community not found");
        }
        c.memberIds.add(user);
        c.lastActivity = "Just now";
        return toDto(c, user);
    }

    public CommunityResponse get(String id, String userId) {
        String user = blankToGuest(userId);
        Community c = communities.get(id);
        if (c == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "community not found");
        }
        if (!visibleTo(c, user)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "community not found");
        }
        return toDto(c, user);
    }

    private static String blankToGuest(String id) {
        if (id == null || id.isBlank()) {
            return "guest";
        }
        return id.trim();
    }

    private static CommunityResponse toDto(Community c, String user) {
        return CommunityResponse.of(
                c.id,
                c.name,
                c.description,
                c.tags,
                c.visibility,
                c.city,
                c.memberIds.size(),
                c.lastActivity,
                c.memberIds.contains(user),
                c.ownerId.equals(user)
        );
    }

    private static final class Community {
        String id;
        String name;
        String description;
        List<String> tags;
        String visibility;
        String city;
        Set<String> memberIds;
        String ownerId;
        String lastActivity;
    }
}
