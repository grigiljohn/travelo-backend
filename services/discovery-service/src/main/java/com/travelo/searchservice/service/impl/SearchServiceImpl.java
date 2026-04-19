package com.travelo.searchservice.service.impl;

import com.travelo.searchservice.document.*;
import com.travelo.searchservice.dto.*;
import com.travelo.searchservice.repository.*;
import com.travelo.searchservice.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

    private final PostDocumentRepository postRepository;
    private final UserDocumentRepository userRepository;
    private final HashtagDocumentRepository hashtagRepository;
    private final LocationDocumentRepository locationRepository;
    private final ShopDocumentRepository shopRepository;
    private final ProductDocumentRepository productRepository;
    private final PrivacyFilterService privacyFilterService;
    private final RelevanceScorerService relevanceScorerService;
    private final SearchResultGroupingService groupingService;
    private final com.travelo.searchservice.client.UserServiceClient userServiceClient;

    public SearchServiceImpl(
            @Lazy PostDocumentRepository postRepository,
            @Lazy UserDocumentRepository userRepository,
            @Lazy HashtagDocumentRepository hashtagRepository,
            @Lazy LocationDocumentRepository locationRepository,
            @Lazy ShopDocumentRepository shopRepository,
            @Lazy ProductDocumentRepository productRepository,
            PrivacyFilterService privacyFilterService,
            RelevanceScorerService relevanceScorerService,
            SearchResultGroupingService groupingService,
            com.travelo.searchservice.client.UserServiceClient userServiceClient) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.hashtagRepository = hashtagRepository;
        this.locationRepository = locationRepository;
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
        this.privacyFilterService = privacyFilterService;
        this.relevanceScorerService = relevanceScorerService;
        this.groupingService = groupingService;
        this.userServiceClient = userServiceClient;
    }

    @Override
    public SearchResponse search(String query, String type, int page, int limit, String viewerId) {
        logger.info("Search query: '{}', type: {}, viewerId: {}", query, type, viewerId);

        if (type == null || type.isEmpty() || "all".equalsIgnoreCase(type)) {
            // Search all types
            return searchAll(query, page, limit, viewerId);
        }

        switch (type.toLowerCase()) {
            case "users":
                return searchUsers(query, page, limit, viewerId);
            case "hashtags":
                return searchHashtags(query, page, limit);
            case "locations":
                return searchLocations(query, page, limit);
            case "posts":
                return searchPosts(query, page, limit, viewerId);
            case "shops":
                return searchShops(query, page, limit);
            case "products":
                return searchProducts(query, page, limit);
            default:
                logger.warn("Unknown search type: {}", type);
                return searchAll(query, page, limit, viewerId);
        }
    }

    @Override
    public GroupedSearchResponse searchGrouped(String query, String types, int page, int limit, String viewerId) {
        logger.info("Grouped search query: '{}', types: {}, viewerId: {}", query, types, viewerId);

        List<SearchResultItem> allResults = new ArrayList<>();
        long totalCount = 0;

        // Parse types to search
        Set<String> typesToSearch = new HashSet<>();
        if (types != null && !types.isEmpty() && !"all".equalsIgnoreCase(types)) {
            String[] typeArray = types.split(",");
            for (String t : typeArray) {
                typesToSearch.add(t.trim().toLowerCase());
            }
        } else {
            typesToSearch.addAll(Arrays.asList("users", "hashtags", "locations", "posts", "shops", "products"));
        }

        // Search each type
        if (typesToSearch.contains("users")) {
            SearchResponse usersResponse = searchUsers(query, page, limit, viewerId);
            allResults.addAll(usersResponse.getResults());
            totalCount += usersResponse.getTotalResults();
        }

        if (typesToSearch.contains("hashtags")) {
            SearchResponse hashtagsResponse = searchHashtags(query, page, limit);
            allResults.addAll(hashtagsResponse.getResults());
            totalCount += hashtagsResponse.getTotalResults();
        }

        if (typesToSearch.contains("locations") || typesToSearch.contains("places")) {
            SearchResponse locationsResponse = searchLocations(query, page, limit);
            allResults.addAll(locationsResponse.getResults());
            totalCount += locationsResponse.getTotalResults();
        }

        if (typesToSearch.contains("posts")) {
            SearchResponse postsResponse = searchPosts(query, page, limit, viewerId);
            allResults.addAll(postsResponse.getResults());
            totalCount += postsResponse.getTotalResults();
        }

        if (typesToSearch.contains("shops")) {
            SearchResponse shopsResponse = searchShops(query, page, limit);
            allResults.addAll(shopsResponse.getResults());
            totalCount += shopsResponse.getTotalResults();
        }

        if (typesToSearch.contains("products")) {
            SearchResponse productsResponse = searchProducts(query, page, limit);
            allResults.addAll(productsResponse.getResults());
            totalCount += productsResponse.getTotalResults();
        }

        // Group results
        Map<String, List<SearchResultItem>> grouped = groupingService.groupResults(allResults);

        return new GroupedSearchResponse(
                grouped.get("top"),
                grouped.get("users"),
                grouped.get("hashtags"),
                grouped.get("places"),
                grouped.get("posts"),
                grouped.get("shops"),
                grouped.get("products"),
                totalCount,
                page,
                limit,
                allResults.size() >= limit
        );
    }

    @Override
    public SearchResponse searchUsers(String query, int page, int limit, String viewerId) {
        logger.info("Searching users with query: '{}', page: {}, limit: {}, viewerId: {}", query, page, limit, viewerId);
        
        List<SearchResultItem> items = new ArrayList<>();
        
        try {
            // Call user-service directly to get users with follow state
            List<com.travelo.searchservice.client.UserServiceClient.UserDto> userDtos = 
                userServiceClient.searchUsersWithFollowState(query, viewerId, page, limit);
            
            // Convert UserDto to SearchResultItem
            for (com.travelo.searchservice.client.UserServiceClient.UserDto userDto : userDtos) {
                SearchResultItem item = new SearchResultItem();
                item.setType("user");
                item.setId(userDto.getId());
                item.setTitle(userDto.getUsername() != null ? userDto.getUsername() : "Unknown");
                item.setSubtitle(userDto.getName() != null ? userDto.getName() : "");
                item.setImageUrl(userDto.getProfilePictureUrl() != null ? userDto.getProfilePictureUrl() : "");
                
                // Set metadata
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("isVerified", userDto.getIsVerified() != null ? userDto.getIsVerified() : false);
                metadata.put("followerCount", userDto.getFollowersCount() != null ? userDto.getFollowersCount() : 0);
                metadata.put("followingCount", userDto.getFollowingCount() != null ? userDto.getFollowingCount() : 0);
                item.setMetadata(metadata);
                
                // Set follow state
                item.setIsFollowing(userDto.getIsFollowing());
                
                // Simple relevance score (username matches are more relevant)
                double score = 10.0;
                if (userDto.getUsername() != null && userDto.getUsername().toLowerCase().contains(query.toLowerCase())) {
                    score += 5.0;
                }
                if (userDto.getName() != null && userDto.getName().toLowerCase().contains(query.toLowerCase())) {
                    score += 3.0;
                }
                item.setRelevanceScore(score);
                
                items.add(item);
            }
            
            // Sort by relevance score
            items.sort((a, b) -> {
                Double scoreA = a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0;
                Double scoreB = b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0;
                return Double.compare(scoreB, scoreA);
            });
            
            logger.info("Found {} users from user-service", items.size());
        } catch (Exception e) {
            logger.error("Failed to search users from user-service: {}", e.getMessage(), e);
            // Fallback to Elasticsearch if user-service fails
            try {
                Pageable pageable = PageRequest.of(page - 1, limit);
                Page<UserDocument> results = userRepository.findByUsernameContainingOrDisplayNameContaining(query, query, pageable);
                
                // Apply privacy filtering
                List<UserDocument> filteredUsers = privacyFilterService.filterUsers(results.getContent(), viewerId);
                
                // Convert to SearchResultItem
                items = filteredUsers.stream()
                        .map(user -> {
                            SearchResultItem item = SearchResultItem.fromUser(user);
                            double score = relevanceScorerService.scoreUser(user, query);
                            item.setRelevanceScore(score);
                            return item;
                        })
                        .sorted((a, b) -> {
                            Double scoreA = a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0;
                            Double scoreB = b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0;
                            return Double.compare(scoreB, scoreA);
                        })
                        .collect(Collectors.toList());
                
                logger.info("Fallback: Found {} users from Elasticsearch", items.size());
            } catch (Exception esException) {
                logger.error("Failed to search users from Elasticsearch: {}", esException.getMessage(), esException);
            }
        }

        return new SearchResponse(items, (long) items.size(), page, limit, items.size() >= limit);
    }

    @Override
    public SearchResponse searchHashtags(String query, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        // Remove # if present
        String searchQuery = query.startsWith("#") ? query.substring(1) : query;
        Page<HashtagDocument> results = hashtagRepository.findByNameContaining(searchQuery, pageable);
        
        // Calculate relevance scores and create result items
        List<SearchResultItem> items = results.getContent().stream()
                .map(hashtag -> {
                    SearchResultItem item = SearchResultItem.fromHashtag(hashtag);
                    double score = relevanceScorerService.scoreHashtag(hashtag, query);
                    item.setRelevanceScore(score);
                    return item;
                })
                .sorted((a, b) -> {
                    Double scoreA = a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0;
                    Double scoreB = b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0;
                    return Double.compare(scoreB, scoreA);
                })
                .collect(Collectors.toList());

        return new SearchResponse(items, results.getTotalElements(), page, limit, results.hasNext());
    }

    @Override
    public SearchResponse searchLocations(String query, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<LocationDocument> results = locationRepository.findByNameContainingOrCityContainingOrCountryContaining(
                query, query, query, pageable);
        
        // Calculate relevance scores and create result items
        List<SearchResultItem> items = results.getContent().stream()
                .map(location -> {
                    SearchResultItem item = SearchResultItem.fromLocation(location);
                    double score = relevanceScorerService.scoreLocation(location, query);
                    item.setRelevanceScore(score);
                    return item;
                })
                .sorted((a, b) -> {
                    Double scoreA = a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0;
                    Double scoreB = b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0;
                    return Double.compare(scoreB, scoreA);
                })
                .collect(Collectors.toList());

        return new SearchResponse(items, results.getTotalElements(), page, limit, results.hasNext());
    }

    @Override
    public SearchResponse searchPosts(String query, int page, int limit, String viewerId) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<PostDocument> results = postRepository.findByCaptionContaining(query, pageable);
        
        // Build user privacy map for filtering
        Map<String, Boolean> userPrivacyMap = new HashMap<>();
        for (PostDocument post : results.getContent()) {
            if (post.getUserId() != null && !userPrivacyMap.containsKey(post.getUserId())) {
                // TODO: Fetch user privacy status from user-service
                // For now, assume all users are public
                userPrivacyMap.put(post.getUserId(), false);
            }
        }
        
        // Apply privacy filtering
        List<PostDocument> filteredPosts = privacyFilterService.filterPosts(results.getContent(), viewerId, userPrivacyMap);
        
        // Calculate relevance scores and create result items
        List<SearchResultItem> items = filteredPosts.stream()
                .map(post -> {
                    SearchResultItem item = SearchResultItem.fromPost(post);
                    double score = relevanceScorerService.scorePost(post, query);
                    item.setRelevanceScore(score);
                    return item;
                })
                .sorted((a, b) -> {
                    Double scoreA = a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0;
                    Double scoreB = b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0;
                    return Double.compare(scoreB, scoreA);
                })
                .collect(Collectors.toList());

        return new SearchResponse(items, (long) items.size(), page, limit, items.size() >= limit);
    }

    private SearchResponse searchAll(String query, int page, int limit, String viewerId) {
        List<SearchResultItem> allResults = new ArrayList<>();
        long totalCount = 0;

        // Search each type and combine results
        SearchResponse users = searchUsers(query, page, limit, viewerId);
        SearchResponse hashtags = searchHashtags(query, page, limit);
        SearchResponse locations = searchLocations(query, page, limit);
        SearchResponse posts = searchPosts(query, page, limit, viewerId);

        // Combine results (take top results from each type)
        int perTypeLimit = limit / 4; // Distribute limit across 4 types
        if (perTypeLimit > 0) {
            allResults.addAll(users.getResults().stream().limit(perTypeLimit).collect(Collectors.toList()));
            allResults.addAll(hashtags.getResults().stream().limit(perTypeLimit).collect(Collectors.toList()));
            allResults.addAll(locations.getResults().stream().limit(perTypeLimit).collect(Collectors.toList()));
            allResults.addAll(posts.getResults().stream().limit(perTypeLimit).collect(Collectors.toList()));
        }

        totalCount = users.getTotalResults() + hashtags.getTotalResults() + 
                    locations.getTotalResults() + posts.getTotalResults();

        return new SearchResponse(allResults, totalCount, page, limit, allResults.size() >= limit);
    }

    @Override
    public SearchResponse searchShops(String query, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<ShopDocument> results = shopRepository.findByNameContainingOrDescriptionContaining(query, query, pageable);
        
        // Filter only active shops
        List<ShopDocument> activeShops = results.getContent().stream()
                .filter(shop -> shop.getIsActive() != null && shop.getIsActive())
                .collect(Collectors.toList());
        
        List<SearchResultItem> items = activeShops.stream()
                .map(shop -> {
                    SearchResultItem item = SearchResultItem.fromShop(shop);
                    // Simple relevance scoring
                    double score = 10.0;
                    if (shop.getName() != null && shop.getName().toLowerCase().contains(query.toLowerCase())) {
                        score += 5.0;
                    }
                    if (shop.getIsVerified() != null && shop.getIsVerified()) {
                        score += 3.0;
                    }
                    if (shop.getProductCount() != null) {
                        score += Math.min(shop.getProductCount() / 10.0, 5.0);
                    }
                    item.setRelevanceScore(score);
                    return item;
                })
                .sorted((a, b) -> {
                    Double scoreA = a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0;
                    Double scoreB = b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0;
                    return Double.compare(scoreB, scoreA);
                })
                .collect(Collectors.toList());

        return new SearchResponse(items, (long) items.size(), page, limit, items.size() >= limit);
    }

    @Override
    public SearchResponse searchProducts(String query, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<ProductDocument> results = productRepository.findByNameContainingOrDescriptionContaining(query, query, pageable);
        
        // Filter only available products
        List<ProductDocument> availableProducts = results.getContent().stream()
                .filter(product -> product.getIsAvailable() != null && product.getIsAvailable())
                .collect(Collectors.toList());
        
        List<SearchResultItem> items = availableProducts.stream()
                .map(product -> {
                    SearchResultItem item = SearchResultItem.fromProduct(product);
                    // Simple relevance scoring
                    double score = 8.0;
                    if (product.getName() != null && product.getName().toLowerCase().contains(query.toLowerCase())) {
                        score += 5.0;
                    }
                    if (product.getIsFeatured() != null && product.getIsFeatured()) {
                        score += 3.0;
                    }
                    if (product.getViewCount() != null) {
                        score += Math.min(product.getViewCount() / 100.0, 3.0);
                    }
                    item.setRelevanceScore(score);
                    return item;
                })
                .sorted((a, b) -> {
                    Double scoreA = a.getRelevanceScore() != null ? a.getRelevanceScore() : 0.0;
                    Double scoreB = b.getRelevanceScore() != null ? b.getRelevanceScore() : 0.0;
                    return Double.compare(scoreB, scoreA);
                })
                .collect(Collectors.toList());

        return new SearchResponse(items, (long) items.size(), page, limit, items.size() >= limit);
    }

    @Override
    public SearchResponse searchReels(int page, int limit, String viewerId) {
        logger.info("Getting reels for explore feed - page={}, limit={}, viewerId={}", page, limit, viewerId);
        
        try {
            Pageable pageable = PageRequest.of(page - 1, limit);
            // Get all posts with postType='reel'
            Page<PostDocument> results = postRepository.findByPostType("reel", pageable);
            
            if (results == null || results.getContent() == null) {
                logger.warn("No reels found in Elasticsearch");
                return new SearchResponse(new ArrayList<>(), 0L, page, limit, false);
            }
            
            // Build user privacy map for filtering
            Map<String, Boolean> userPrivacyMap = new HashMap<>();
            for (PostDocument post : results.getContent()) {
                if (post != null && post.getUserId() != null && !userPrivacyMap.containsKey(post.getUserId())) {
                    // TODO: Fetch user privacy status from user-service
                    // For now, assume all users are public
                    userPrivacyMap.put(post.getUserId(), false);
                }
            }
            
            // Apply privacy filtering
            List<PostDocument> filteredReels = privacyFilterService.filterPosts(results.getContent(), viewerId, userPrivacyMap);
            
            // Convert to SearchResultItem and sort by creation date (newest first)
            List<SearchResultItem> items = filteredReels.stream()
                    .filter(post -> post != null) // Filter out null posts
                    .map(post -> {
                        try {
                            return SearchResultItem.fromPost(post);
                        } catch (Exception e) {
                            logger.error("Error converting post {} to SearchResultItem: {}", 
                                    post != null ? post.getId() : "null", e.getMessage(), e);
                            return null;
                        }
                    })
                    .filter(item -> item != null) // Filter out failed conversions
                    .sorted((a, b) -> {
                        // Sort by creation date if available (newest first)
                        try {
                            String createdAtA = (String) a.getMetadata().get("createdAt");
                            String createdAtB = (String) b.getMetadata().get("createdAt");
                            if (createdAtA != null && createdAtB != null) {
                                return createdAtB.compareTo(createdAtA); // Descending order (newest first)
                            }
                        } catch (Exception e) {
                            logger.debug("Error sorting by createdAt: {}", e.getMessage());
                        }
                        return 0; // Keep original order if sorting fails
                    })
                    .collect(Collectors.toList());
            
            logger.info("Found {} reels (filtered from {} total)", items.size(), results.getTotalElements());
            
            return new SearchResponse(items, (long) items.size(), page, limit, items.size() >= limit);
        } catch (Exception e) {
            logger.error("Error in searchReels: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch reels: " + e.getMessage(), e);
        }
    }

    @Override
    public SearchResponse getFollowingFeed(int page, int limit, String userId) {
        logger.info("Getting following feed - page={}, limit={}, userId={}", page, limit, userId);
        
        try {
            // TODO: Fetch list of user IDs that the current user follows from user-service
            // For now, return all reels (will be enhanced when follow relationship is implemented)
            // This matches the requirement: "If no people they are following, list all people from database"
            
            Pageable pageable = PageRequest.of(page - 1, limit);
            Page<PostDocument> results = postRepository.findByPostType("reel", pageable);
            
            if (results == null || results.getContent() == null) {
                logger.warn("No reels found for following feed");
                return new SearchResponse(new ArrayList<>(), 0L, page, limit, false);
            }
            
            // Build user privacy map
            Map<String, Boolean> userPrivacyMap = new HashMap<>();
            for (PostDocument post : results.getContent()) {
                if (post != null && post.getUserId() != null && !userPrivacyMap.containsKey(post.getUserId())) {
                    userPrivacyMap.put(post.getUserId(), false); // Assume public for now
                }
            }
            
            // Apply privacy filtering
            List<PostDocument> filteredReels = privacyFilterService.filterPosts(results.getContent(), userId, userPrivacyMap);
            
            // Convert to SearchResultItem
            List<SearchResultItem> items = filteredReels.stream()
                    .filter(post -> post != null)
                    .map(post -> {
                        try {
                            return SearchResultItem.fromPost(post);
                        } catch (Exception e) {
                            logger.error("Error converting post {} to SearchResultItem: {}", 
                                    post != null ? post.getId() : "null", e.getMessage(), e);
                            return null;
                        }
                    })
                    .filter(item -> item != null)
                    .sorted((a, b) -> {
                        // Sort by creation date (newest first)
                        try {
                            String createdAtA = (String) a.getMetadata().get("createdAt");
                            String createdAtB = (String) b.getMetadata().get("createdAt");
                            if (createdAtA != null && createdAtB != null) {
                                return createdAtB.compareTo(createdAtA);
                            }
                        } catch (Exception e) {
                            logger.debug("Error sorting by createdAt: {}", e.getMessage());
                        }
                        return 0;
                    })
                    .collect(Collectors.toList());
            
            logger.info("Found {} reels for following feed", items.size());
            return new SearchResponse(items, (long) items.size(), page, limit, items.size() >= limit);
        } catch (Exception e) {
            logger.error("Error in getFollowingFeed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch following feed: " + e.getMessage(), e);
        }
    }

    @Override
    public SearchResponse getExploreFeed(int page, int limit, String userId) {
        logger.info("Getting explore feed - page={}, limit={}, userId={}", page, limit, userId);
        
        try {
            // Get all reels and shuffle for explore feed
            // TODO: Enhance with user preferences, location, and other factors
            Pageable pageable = PageRequest.of(page - 1, limit * 2); // Get more to shuffle
            Page<PostDocument> results = postRepository.findByPostType("reel", pageable);
            
            if (results == null || results.getContent() == null) {
                logger.warn("No reels found for explore feed");
                return new SearchResponse(new ArrayList<>(), 0L, page, limit, false);
            }
            
            // Build user privacy map
            Map<String, Boolean> userPrivacyMap = new HashMap<>();
            for (PostDocument post : results.getContent()) {
                if (post != null && post.getUserId() != null && !userPrivacyMap.containsKey(post.getUserId())) {
                    userPrivacyMap.put(post.getUserId(), false);
                }
            }
            
            // Apply privacy filtering
            List<PostDocument> filteredReels = privacyFilterService.filterPosts(results.getContent(), userId, userPrivacyMap);
            
            // Shuffle for explore feed randomness
            Collections.shuffle(filteredReels);
            
            // Limit to requested size
            if (filteredReels.size() > limit) {
                filteredReels = filteredReels.subList(0, limit);
            }
            
            // Convert to SearchResultItem
            List<SearchResultItem> items = filteredReels.stream()
                    .filter(post -> post != null)
                    .map(post -> {
                        try {
                            return SearchResultItem.fromPost(post);
                        } catch (Exception e) {
                            logger.error("Error converting post {} to SearchResultItem: {}", 
                                    post != null ? post.getId() : "null", e.getMessage(), e);
                            return null;
                        }
                    })
                    .filter(item -> item != null)
                    .collect(Collectors.toList());
            
            logger.info("Found {} reels for explore feed", items.size());
            return new SearchResponse(items, (long) items.size(), page, limit, items.size() >= limit);
        } catch (Exception e) {
            logger.error("Error in getExploreFeed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch explore feed: " + e.getMessage(), e);
        }
    }

    @Override
    public SearchResponse getNearbyFeed(Double latitude, Double longitude, int page, int limit, String userId) {
        logger.info("Getting nearby feed - lat={}, lng={}, page={}, limit={}, userId={}", 
                latitude, longitude, page, limit, userId);
        
        try {
            Pageable pageable = PageRequest.of(page - 1, limit);
            
            // If location is provided, filter by location
            // Otherwise, return all reels (will be enhanced with location-based filtering)
            Page<PostDocument> results;
            if (latitude != null && longitude != null) {
                // TODO: Implement location-based filtering using Elasticsearch geo queries
                // For now, filter by location field containing text
                results = postRepository.findByPostType("reel", pageable);
                // Filter by location if available
                List<PostDocument> locationFiltered = results.getContent().stream()
                        .filter(post -> post != null && post.getLocation() != null && !post.getLocation().isEmpty())
                        .collect(Collectors.toList());
                results = new org.springframework.data.domain.PageImpl<>(
                        locationFiltered, pageable, locationFiltered.size());
            } else {
                // No location provided, return all reels
                results = postRepository.findByPostType("reel", pageable);
            }
            
            if (results == null || results.getContent() == null) {
                logger.warn("No reels found for nearby feed");
                return new SearchResponse(new ArrayList<>(), 0L, page, limit, false);
            }
            
            // Build user privacy map
            Map<String, Boolean> userPrivacyMap = new HashMap<>();
            for (PostDocument post : results.getContent()) {
                if (post != null && post.getUserId() != null && !userPrivacyMap.containsKey(post.getUserId())) {
                    userPrivacyMap.put(post.getUserId(), false);
                }
            }
            
            // Apply privacy filtering
            List<PostDocument> filteredReels = privacyFilterService.filterPosts(results.getContent(), userId, userPrivacyMap);
            
            // Convert to SearchResultItem
            List<SearchResultItem> items = filteredReels.stream()
                    .filter(post -> post != null)
                    .map(post -> {
                        try {
                            return SearchResultItem.fromPost(post);
                        } catch (Exception e) {
                            logger.error("Error converting post {} to SearchResultItem: {}", 
                                    post != null ? post.getId() : "null", e.getMessage(), e);
                            return null;
                        }
                    })
                    .filter(item -> item != null)
                    .sorted((a, b) -> {
                        // Sort by creation date (newest first)
                        try {
                            String createdAtA = (String) a.getMetadata().get("createdAt");
                            String createdAtB = (String) b.getMetadata().get("createdAt");
                            if (createdAtA != null && createdAtB != null) {
                                return createdAtB.compareTo(createdAtA);
                            }
                        } catch (Exception e) {
                            logger.debug("Error sorting by createdAt: {}", e.getMessage());
                        }
                        return 0;
                    })
                    .collect(Collectors.toList());
            
            logger.info("Found {} reels for nearby feed", items.size());
            return new SearchResponse(items, (long) items.size(), page, limit, items.size() >= limit);
        } catch (Exception e) {
            logger.error("Error in getNearbyFeed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch nearby feed: " + e.getMessage(), e);
        }
    }

    @Override
    public SearchResponse getUserSuggestions(int page, int limit, String userId) {
        logger.info("Getting user suggestions - page={}, limit={}, userId={}", page, limit, userId);
        
        try {
            Pageable pageable = PageRequest.of(page - 1, limit);
            
            // Get all users (will be enhanced to exclude followed users and add recommendations)
            Page<UserDocument> results = userRepository.findAll(pageable);
            
            if (results == null || results.getContent() == null) {
                logger.warn("No users found for suggestions");
                return new SearchResponse(new ArrayList<>(), 0L, page, limit, false);
            }
            
            // Convert to SearchResultItem
            List<SearchResultItem> items = results.getContent().stream()
                    .filter(user -> user != null)
                    .filter(user -> userId == null || !user.getId().equals(userId)) // Exclude current user
                    .map(user -> {
                        try {
                            return SearchResultItem.fromUser(user);
                        } catch (Exception e) {
                            logger.error("Error converting user {} to SearchResultItem: {}", 
                                    user != null ? user.getId() : "null", e.getMessage(), e);
                            return null;
                        }
                    })
                    .filter(item -> item != null)
                    .collect(Collectors.toList());
            
            logger.info("Found {} user suggestions", items.size());
            return new SearchResponse(items, (long) items.size(), page, limit, items.size() >= limit);
        } catch (Exception e) {
            logger.error("Error in getUserSuggestions: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch user suggestions: " + e.getMessage(), e);
        }
    }
}

