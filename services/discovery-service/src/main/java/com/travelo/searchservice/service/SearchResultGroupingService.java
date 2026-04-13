package com.travelo.searchservice.service;

import com.travelo.searchservice.dto.SearchResultItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for grouping and ranking search results by type.
 * Groups results into: Top, Users, Hashtags, Places, Posts.
 */
@Service
public class SearchResultGroupingService {

    private static final Logger logger = LoggerFactory.getLogger(SearchResultGroupingService.class);

    private static final int TOP_RESULTS_LIMIT = 5;

    /**
     * Group search results by type and select top results.
     * 
     * @param allResults All search results (may contain multiple types)
     * @return Map with keys: "top", "users", "hashtags", "places", "posts"
     */
    public Map<String, List<SearchResultItem>> groupResults(List<SearchResultItem> allResults) {
        logger.debug("Grouping {} search results by type", allResults.size());

        // Separate results by type
        List<SearchResultItem> users = new ArrayList<>();
        List<SearchResultItem> hashtags = new ArrayList<>();
        List<SearchResultItem> places = new ArrayList<>();
        List<SearchResultItem> posts = new ArrayList<>();
        List<SearchResultItem> shops = new ArrayList<>();
        List<SearchResultItem> products = new ArrayList<>();

        for (SearchResultItem result : allResults) {
            switch (result.getType()) {
                case "user":
                    users.add(result);
                    break;
                case "hashtag":
                    hashtags.add(result);
                    break;
                case "location":
                    places.add(result);
                    break;
                case "post":
                    posts.add(result);
                    break;
                case "shop":
                    shops.add(result);
                    break;
                case "product":
                    products.add(result);
                    break;
                default:
                    logger.warn("Unknown result type: {}", result.getType());
            }
        }

        // Sort each type by relevance score (descending)
        users.sort((a, b) -> {
            Double scoreA = a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0;
            Double scoreB = b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0;
            return Double.compare(scoreB, scoreA);
        });

        hashtags.sort((a, b) -> {
            Double scoreA = a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0;
            Double scoreB = b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0;
            return Double.compare(scoreB, scoreA);
        });

        places.sort((a, b) -> {
            Double scoreA = a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0;
            Double scoreB = b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0;
            return Double.compare(scoreB, scoreA);
        });

        posts.sort((a, b) -> {
            Double scoreA = a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0;
            Double scoreB = b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0;
            return Double.compare(scoreB, scoreA);
        });

        shops.sort((a, b) -> {
            Double scoreA = a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0;
            Double scoreB = b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0;
            return Double.compare(scoreB, scoreA);
        });

        products.sort((a, b) -> {
            Double scoreA = a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0;
            Double scoreB = b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0;
            return Double.compare(scoreB, scoreA);
        });

        // Select top results across all types
        List<SearchResultItem> top = selectTopResults(allResults, TOP_RESULTS_LIMIT);

        Map<String, List<SearchResultItem>> grouped = new HashMap<>();
        grouped.put("top", top);
        grouped.put("users", users);
        grouped.put("hashtags", hashtags);
        grouped.put("places", places);
        grouped.put("posts", posts);
        grouped.put("shops", shops);
        grouped.put("products", products);

        logger.debug("Grouped results - Top: {}, Users: {}, Hashtags: {}, Places: {}, Posts: {}, Shops: {}, Products: {}",
                top.size(), users.size(), hashtags.size(), places.size(), posts.size(), shops.size(), products.size());

        return grouped;
    }

    /**
     * Select top N results across all types based on relevance score.
     * 
     * @param allResults All search results
     * @param limit Maximum number of top results to return
     * @return Top N results sorted by relevance score
     */
    public List<SearchResultItem> selectTopResults(List<SearchResultItem> allResults, int limit) {
        if (allResults == null || allResults.isEmpty()) {
            return new ArrayList<>();
        }

        // Sort all results by relevance score (descending)
        List<SearchResultItem> sorted = new ArrayList<>(allResults);
        sorted.sort((a, b) -> {
            Double scoreA = a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0;
            Double scoreB = b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0;
            return Double.compare(scoreB, scoreA);
        });

        // Return top N results
        return sorted.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}

