package com.travelo.searchservice.seed;

import com.travelo.searchservice.dto.SearchResultItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Curated {@link SearchResultItem} rows shaped like {@code SearchResultItem.fromPost}
 * so mobile grids (Explore → Inspire) stay populated when Elasticsearch has no reels yet.
 */
public final class ExploreFeedSeed {

    private ExploreFeedSeed() {
    }

    private static SearchResultItem post(
            String id,
            String caption,
            String username,
            String thumb,
            String video,
            String location
    ) {
        SearchResultItem item = new SearchResultItem();
        item.setType("post");
        item.setId(id);
        String title = caption.length() > 52 ? caption.substring(0, 49) + "…" : caption;
        item.setTitle(title);
        item.setSubtitle("by @" + username);
        item.setImageUrl(thumb);
        Map<String, Object> md = new HashMap<>();
        md.put("userId", "seed_" + username);
        md.put("username", username);
        md.put("caption", caption);
        md.put("likes", 128);
        md.put("comments", 14);
        md.put("shares", 3);
        md.put("location", location);
        md.put("tags", List.of("travel", "explore"));
        md.put("thumbnailUrl", thumb);
        md.put("videoUrl", video != null ? video : "");
        md.put("mediaUrls", video != null && !video.isEmpty() ? List.of(video) : List.of());
        md.put("userAvatar", "https://picsum.photos/seed/" + username + "-ava/200/200");
        md.put("isLiked", false);
        item.setMetadata(md);
        return item;
    }

    /**
     * @param maxCount clamped internally to a small catalog size
     */
    public static List<SearchResultItem> posts(int maxCount) {
        int n = Math.min(Math.max(maxCount, 1), 12);
        List<SearchResultItem> all = new ArrayList<>(List.of(
                post(
                        "seed-explore-1",
                        "Golden hour over the caldera — Santorini never gets old.",
                        "maya_wanders",
                        "https://images.unsplash.com/photo-1613395877344-13d4c79e4284?w=900&q=80",
                        "",
                        "Oía, Greece"
                ),
                post(
                        "seed-explore-2",
                        "Street noodles in Bangkok: spicy, smoky, perfect.",
                        "arjun_eats",
                        "https://images.unsplash.com/photo-1559314809-0d155014e29e?w=900&q=80",
                        "",
                        "Bangkok, Thailand"
                ),
                post(
                        "seed-explore-3",
                        "First tracks in the Dolomites. Worth the 4am alarm.",
                        "sofia_lens",
                        "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=900&q=80",
                        "",
                        "Cortina, Italy"
                ),
                post(
                        "seed-explore-4",
                        "Desert dunes at blue hour — quiet like nowhere else.",
                        "jon_roams",
                        "https://images.unsplash.com/photo-1509316785289-025f5f846375?w=900&q=80",
                        "",
                        "Wadi Rum, Jordan"
                ),
                post(
                        "seed-explore-5",
                        "Kyoto alleyways after rain. Neon + reflections.",
                        "maya_wanders",
                        "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=900&q=80",
                        "",
                        "Kyoto, Japan"
                ),
                post(
                        "seed-explore-6",
                        "Cliffside trail in Madeira — clouds below your feet.",
                        "sofia_lens",
                        "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=900&q=80",
                        "",
                        "Madeira, Portugal"
                ),
                post(
                        "seed-explore-7",
                        "Night market energy: lights, sizzle, and mango sticky rice.",
                        "arjun_eats",
                        "https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=900&q=80",
                        "",
                        "Chiang Mai, Thailand"
                ),
                post(
                        "seed-explore-8",
                        "Fjord ferry commute beats any subway line.",
                        "jon_roams",
                        "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=900&q=80",
                        "",
                        "Geiranger, Norway"
                )
        ));
        return all.subList(0, Math.min(n, all.size()));
    }
}
