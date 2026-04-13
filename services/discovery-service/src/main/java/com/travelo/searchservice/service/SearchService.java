package com.travelo.searchservice.service;

import com.travelo.searchservice.dto.GroupedSearchResponse;
import com.travelo.searchservice.dto.SearchResponse;

/**
 * Service for full-text search across users, hashtags, locations, and posts.
 */
public interface SearchService {
    
    /**
     * Search across all types or specific type.
     * @param query Search query
     * @param type Optional: users, hashtags, locations, posts (or null for all)
     * @param page Page number
     * @param limit Page size
     * @param viewerId Optional: User ID for privacy filtering
     * @return SearchResponse with results
     */
    SearchResponse search(String query, String type, int page, int limit, String viewerId);
    
    /**
     * Search with grouped results (Top, Users, Hashtags, Places, Posts).
     * @param query Search query
     * @param types Optional: Comma-separated list of types to search
     * @param page Page number
     * @param limit Page size
     * @param viewerId Optional: User ID for privacy filtering
     * @return GroupedSearchResponse with grouped results
     */
    GroupedSearchResponse searchGrouped(String query, String types, int page, int limit, String viewerId);
    
    /**
     * Search users only.
     */
    SearchResponse searchUsers(String query, int page, int limit, String viewerId);
    
    /**
     * Search hashtags only.
     */
    SearchResponse searchHashtags(String query, int page, int limit);
    
    /**
     * Search locations only.
     */
    SearchResponse searchLocations(String query, int page, int limit);
    
    /**
     * Search posts only.
     */
    SearchResponse searchPosts(String query, int page, int limit, String viewerId);
    
    /**
     * Search shops only.
     */
    SearchResponse searchShops(String query, int page, int limit);
    
    /**
     * Search products only.
     */
    SearchResponse searchProducts(String query, int page, int limit);
    
    /**
     * Get reels (posts with postType='reel') for explore/discovery feed.
     * This is used when no search query is provided.
     * @param page Page number
     * @param limit Page size
     * @param viewerId Optional: User ID for privacy filtering
     * @return SearchResponse with reel results
     */
    SearchResponse searchReels(int page, int limit, String viewerId);

    /**
     * Get following feed - reels from users that the current user follows.
     * @param page Page number
     * @param limit Page size
     * @param userId User ID (required)
     * @return SearchResponse with reel results
     */
    SearchResponse getFollowingFeed(int page, int limit, String userId);

    /**
     * Get explore feed - generic feed based on user preferences.
     * @param page Page number
     * @param limit Page size
     * @param userId Optional: User ID for personalization
     * @return SearchResponse with reel results
     */
    SearchResponse getExploreFeed(int page, int limit, String userId);

    /**
     * Get nearby feed - feed based on user location.
     * @param latitude Latitude
     * @param longitude Longitude
     * @param page Page number
     * @param limit Page size
     * @param userId Optional: User ID
     * @return SearchResponse with reel results
     */
    SearchResponse getNearbyFeed(Double latitude, Double longitude, int page, int limit, String userId);

    /**
     * Get user suggestions - users that the current user can follow.
     * @param page Page number
     * @param limit Page size
     * @param userId Optional: User ID to exclude from suggestions
     * @return SearchResponse with user results
     */
    SearchResponse getUserSuggestions(int page, int limit, String userId);
}

