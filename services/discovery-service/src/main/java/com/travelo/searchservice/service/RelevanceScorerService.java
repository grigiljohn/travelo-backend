package com.travelo.searchservice.service;

import com.travelo.searchservice.document.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Service for calculating relevance scores for search results.
 * 
 * Scoring Factors:
 * - Users: Exact username match (+10), username starts with query (+5), display name match (+3), bio match (+1), follower count (+0.5), verified (+2)
 * - Posts: Caption exact match (+5), caption contains query (+2), hashtag match (+3), location match (+1), likes (+0.5), recent posts (+1)
 * - Hashtags: Exact name match (+5), name starts with query (+3), post count (+1), trending score (+2)
 * - Locations: Exact name match (+5), city match (+3), country match (+1), post count (+0.5)
 */
@Service
public class RelevanceScorerService {

    private static final Logger logger = LoggerFactory.getLogger(RelevanceScorerService.class);

    private static final double USERNAME_EXACT_MATCH_BOOST = 10.0;
    private static final double USERNAME_PREFIX_MATCH_BOOST = 5.0;
    private static final double DISPLAY_NAME_MATCH_BOOST = 3.0;
    private static final double BIO_MATCH_BOOST = 1.0;
    private static final double FOLLOWER_COUNT_BOOST = 0.5;
    private static final double VERIFIED_BOOST = 2.0;

    private static final double CAPTION_EXACT_MATCH_BOOST = 5.0;
    private static final double CAPTION_CONTAINS_BOOST = 2.0;
    private static final double HASHTAG_MATCH_BOOST = 3.0;
    private static final double LOCATION_MATCH_BOOST = 1.0;
    private static final double LIKES_BOOST = 0.5;
    private static final double RECENT_POST_BOOST = 1.0;

    private static final double HASHTAG_EXACT_MATCH_BOOST = 5.0;
    private static final double HASHTAG_PREFIX_MATCH_BOOST = 3.0;
    private static final double HASHTAG_POST_COUNT_BOOST = 1.0;
    private static final double HASHTAG_TRENDING_BOOST = 2.0;

    private static final double LOCATION_EXACT_MATCH_BOOST = 5.0;
    private static final double LOCATION_CITY_MATCH_BOOST = 3.0;
    private static final double LOCATION_COUNTRY_MATCH_BOOST = 1.0;
    private static final double LOCATION_POST_COUNT_BOOST = 0.5;

    /**
     * Calculate relevance score for a user document.
     */
    public double scoreUser(UserDocument user, String query) {
        if (query == null || query.isEmpty()) {
            return 0.0;
        }

        String queryLower = query.toLowerCase().trim();
        double score = 0.0;

        // Username matching (highest priority)
        if (user.getUsername() != null) {
            String usernameLower = user.getUsername().toLowerCase();
            if (usernameLower.equals(queryLower)) {
                score += USERNAME_EXACT_MATCH_BOOST;
            } else if (usernameLower.startsWith(queryLower)) {
                score += USERNAME_PREFIX_MATCH_BOOST;
            } else if (usernameLower.contains(queryLower)) {
                score += USERNAME_PREFIX_MATCH_BOOST * 0.5; // Partial match
            }
        }

        // Display name matching
        if (user.getDisplayName() != null) {
            String displayNameLower = user.getDisplayName().toLowerCase();
            if (displayNameLower.contains(queryLower)) {
                score += DISPLAY_NAME_MATCH_BOOST;
            }
        }

        // Bio matching
        if (user.getBio() != null) {
            String bioLower = user.getBio().toLowerCase();
            if (bioLower.contains(queryLower)) {
                score += BIO_MATCH_BOOST;
            }
        }

        // Follower count boost (normalized: 0-1 based on max followers)
        if (user.getFollowerCount() != null && user.getFollowerCount() > 0) {
            double normalizedFollowers = Math.min(1.0, Math.log10(user.getFollowerCount() + 1) / 6.0); // log10(1M) ≈ 6
            score += normalizedFollowers * FOLLOWER_COUNT_BOOST;
        }

        // Verified badge boost
        if (user.getIsVerified() != null && user.getIsVerified()) {
            score += VERIFIED_BOOST;
        }

        return score;
    }

    /**
     * Calculate relevance score for a post document.
     */
    public double scorePost(PostDocument post, String query) {
        if (query == null || query.isEmpty()) {
            return 0.0;
        }

        String queryLower = query.toLowerCase().trim();
        double score = 0.0;

        // Caption matching
        if (post.getCaption() != null) {
            String captionLower = post.getCaption().toLowerCase();
            if (captionLower.equals(queryLower)) {
                score += CAPTION_EXACT_MATCH_BOOST;
            } else if (captionLower.contains(queryLower)) {
                score += CAPTION_CONTAINS_BOOST;
            }
        }

        // Hashtag matching
        if (post.getTags() != null) {
            for (String tag : post.getTags()) {
                String tagLower = tag.toLowerCase();
                if (tagLower.contains(queryLower)) {
                    score += HASHTAG_MATCH_BOOST;
                    break; // Only count once
                }
            }
        }

        // Location matching
        if (post.getLocation() != null) {
            String locationLower = post.getLocation().toLowerCase();
            if (locationLower.contains(queryLower)) {
                score += LOCATION_MATCH_BOOST;
            }
        }

        // Likes boost (normalized)
        if (post.getLikes() != null && post.getLikes() > 0) {
            double normalizedLikes = Math.min(1.0, Math.log10(post.getLikes() + 1) / 5.0); // log10(100K) ≈ 5
            score += normalizedLikes * LIKES_BOOST;
        }

        // Recent post boost (within 7 days)
        if (post.getCreatedAt() != null) {
            long daysAgo = ChronoUnit.DAYS.between(post.getCreatedAt(), OffsetDateTime.now());
            if (daysAgo <= 7) {
                score += RECENT_POST_BOOST * (1.0 - (daysAgo / 7.0)); // Decay over 7 days
            }
        }

        return score;
    }

    /**
     * Calculate relevance score for a hashtag document.
     */
    public double scoreHashtag(HashtagDocument hashtag, String query) {
        if (query == null || query.isEmpty()) {
            return 0.0;
        }

        String queryLower = query.toLowerCase().trim();
        // Remove # from query if present
        if (queryLower.startsWith("#")) {
            queryLower = queryLower.substring(1);
        }

        double score = 0.0;

        // Name matching
        if (hashtag.getName() != null) {
            String nameLower = hashtag.getName().toLowerCase();
            if (nameLower.equals(queryLower)) {
                score += HASHTAG_EXACT_MATCH_BOOST;
            } else if (nameLower.startsWith(queryLower)) {
                score += HASHTAG_PREFIX_MATCH_BOOST;
            } else if (nameLower.contains(queryLower)) {
                score += HASHTAG_PREFIX_MATCH_BOOST * 0.5; // Partial match
            }
        }

        // Post count boost (normalized)
        if (hashtag.getPostCount() != null && hashtag.getPostCount() > 0) {
            double normalizedPostCount = Math.min(1.0, Math.log10(hashtag.getPostCount() + 1) / 5.0);
            score += normalizedPostCount * HASHTAG_POST_COUNT_BOOST;
        }

        // TODO: Trending score boost (requires trending algorithm)
        // For now, we can use recent post count growth
        // score += calculateTrendingScore(hashtag) * HASHTAG_TRENDING_BOOST;

        return score;
    }

    /**
     * Calculate relevance score for a location document.
     */
    public double scoreLocation(LocationDocument location, String query) {
        if (query == null || query.isEmpty()) {
            return 0.0;
        }

        String queryLower = query.toLowerCase().trim();
        double score = 0.0;

        // Name matching
        if (location.getName() != null) {
            String nameLower = location.getName().toLowerCase();
            if (nameLower.equals(queryLower)) {
                score += LOCATION_EXACT_MATCH_BOOST;
            } else if (nameLower.contains(queryLower)) {
                score += LOCATION_EXACT_MATCH_BOOST * 0.7; // Partial match
            }
        }

        // City matching
        if (location.getCity() != null) {
            String cityLower = location.getCity().toLowerCase();
            if (cityLower.contains(queryLower)) {
                score += LOCATION_CITY_MATCH_BOOST;
            }
        }

        // Country matching
        if (location.getCountry() != null) {
            String countryLower = location.getCountry().toLowerCase();
            if (countryLower.contains(queryLower)) {
                score += LOCATION_COUNTRY_MATCH_BOOST;
            }
        }

        // Post count boost (normalized)
        if (location.getPostCount() != null && location.getPostCount() > 0) {
            double normalizedPostCount = Math.min(1.0, Math.log10(location.getPostCount() + 1) / 4.0);
            score += normalizedPostCount * LOCATION_POST_COUNT_BOOST;
        }

        return score;
    }
}

