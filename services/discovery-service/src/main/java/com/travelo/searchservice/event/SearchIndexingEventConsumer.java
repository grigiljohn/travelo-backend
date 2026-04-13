package com.travelo.searchservice.event;

import com.travelo.searchservice.document.*;
import com.travelo.searchservice.service.SearchIndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer for indexing events.
 * Listens to post.created, user.updated, tag.created events and updates ES indexes.
 */
@Component
public class SearchIndexingEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(SearchIndexingEventConsumer.class);

    private final SearchIndexingService indexingService;

    public SearchIndexingEventConsumer(SearchIndexingService indexingService) {
        this.indexingService = indexingService;
    }

    /**
     * Handle post.created event.
     * Index the new post in Elasticsearch.
     */
    @KafkaListener(topics = "post.created", groupId = "discovery-service-group")
    public void handlePostCreated(Map<String, Object> event) {
        try {
            logger.info("Received post.created event: {}", event);
            
            String postId = (String) event.get("postId");
            String userId = (String) event.get("authorId");
            String caption = (String) event.get("caption");
            String postType = (String) event.get("postType");
            String mood = (String) event.get("mood");
            String location = (String) event.get("location");
            
            if (postId == null) {
                logger.warn("Invalid post.created event: missing postId");
                return;
            }

            PostDocument postDoc = new PostDocument();
            postDoc.setId(postId);
            postDoc.setUserId(userId);
            postDoc.setCaption(caption);
            postDoc.setPostType(postType);
            postDoc.setMood(mood);
            postDoc.setLocation(location);
            
            // Extract username from event if available
            String username = (String) event.get("username");
            if (username != null) {
                postDoc.setUsername(username);
            }
            
            // Extract image URLs from event
            // Event may contain: thumbnailUrl, mediaUrls, or mediaItems array
            String thumbnailUrl = (String) event.get("thumbnailUrl");
            if (thumbnailUrl == null && event.get("mediaItems") instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> mediaItems = (List<Map<String, Object>>) event.get("mediaItems");
                if (mediaItems != null && !mediaItems.isEmpty()) {
                    Map<String, Object> firstMedia = mediaItems.get(0);
                    thumbnailUrl = (String) firstMedia.get("thumbnailUrl");
                    if (thumbnailUrl == null) {
                        thumbnailUrl = (String) firstMedia.get("downloadUrl");
                    }
                    // Extract all media URLs
                    List<String> mediaUrls = mediaItems.stream()
                            .map(item -> (String) item.get("downloadUrl"))
                            .filter(url -> url != null)
                            .collect(java.util.stream.Collectors.toList());
                    postDoc.setMediaUrls(mediaUrls);
                }
            }
            postDoc.setThumbnailUrl(thumbnailUrl);
            
            // Extract tags from caption or event
            if (caption != null) {
                List<String> tags = extractHashtags(caption);
                postDoc.setTags(tags);
            }
            
            // Extract engagement metrics from event if available
            Integer likes = event.get("likes") != null ? (Integer) event.get("likes") : 0;
            Integer comments = event.get("comments") != null ? (Integer) event.get("comments") : 0;
            Integer shares = event.get("shares") != null ? (Integer) event.get("shares") : 0;
            
            postDoc.setLikes(likes);
            postDoc.setComments(comments);
            postDoc.setShares(shares);
            postDoc.setCreatedAt(OffsetDateTime.now());
            postDoc.setUpdatedAt(OffsetDateTime.now());

            indexingService.indexPost(postDoc);
            logger.info("Indexed post: {}", postId);

            // Index hashtags if present
            if (caption != null) {
                List<String> tags = extractHashtags(caption);
                for (String tag : tags) {
                    indexHashtagIfNew(tag);
                }
            }

            // Index location if present
            if (location != null && !location.isEmpty()) {
                indexLocationIfNew(location);
            }

        } catch (Exception e) {
            logger.error("Error handling post.created event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle user.updated event.
     * Update user document in Elasticsearch.
     */
    @KafkaListener(topics = "user.updated", groupId = "discovery-service-group")
    public void handleUserUpdated(Map<String, Object> event) {
        try {
            logger.info("Received user.updated event: {}", event);
            
            String userId = (String) event.get("userId");
            String username = (String) event.get("username");
            String displayName = (String) event.get("displayName");
            String bio = (String) event.get("bio");
            String location = (String) event.get("location");
            
            if (userId == null) {
                logger.warn("Invalid user.updated event: missing userId");
                return;
            }

            UserDocument userDoc = new UserDocument();
            userDoc.setId(userId);
            userDoc.setUsername(username);
            userDoc.setDisplayName(displayName);
            userDoc.setBio(bio);
            userDoc.setLocation(location);
            
            // Extract profile picture URL from event
            String profilePictureUrl = (String) event.get("profilePictureUrl");
            if (profilePictureUrl == null) {
                profilePictureUrl = (String) event.get("profile_picture_url");
            }
            userDoc.setProfilePictureUrl(profilePictureUrl);
            
            // Extract privacy and verification status
            Boolean isPrivate = event.get("isPrivate") != null ? (Boolean) event.get("isPrivate") : 
                               (event.get("is_private") != null ? (Boolean) event.get("is_private") : false);
            Boolean isVerified = event.get("isVerified") != null ? (Boolean) event.get("isVerified") :
                                (event.get("is_verified") != null ? (Boolean) event.get("is_verified") : false);
            userDoc.setIsPrivate(isPrivate);
            userDoc.setIsVerified(isVerified);
            
            // Extract follower/following counts if available
            Integer followerCount = event.get("followerCount") != null ? (Integer) event.get("followerCount") :
                                   (event.get("followers_count") != null ? (Integer) event.get("followers_count") : 0);
            Integer followingCount = event.get("followingCount") != null ? (Integer) event.get("followingCount") :
                                    (event.get("following_count") != null ? (Integer) event.get("following_count") : 0);
            userDoc.setFollowerCount(followerCount);
            userDoc.setFollowingCount(followingCount);
            
            userDoc.setUpdatedAt(OffsetDateTime.now());

            indexingService.indexUser(userDoc);
            logger.info("Indexed user: {}", userId);

        } catch (Exception e) {
            logger.error("Error handling user.updated event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle tag.created event.
     * Index the new hashtag in Elasticsearch.
     */
    @KafkaListener(topics = "tag.created", groupId = "discovery-service-group")
    public void handleTagCreated(Map<String, Object> event) {
        try {
            logger.info("Received tag.created event: {}", event);
            
            String tagId = (String) event.get("tagId");
            String tag = (String) event.get("tag");
            Integer postCount = event.get("postCount") != null ? (Integer) event.get("postCount") : 0;
            
            if (tag == null) {
                logger.warn("Invalid tag.created event: missing tag");
                return;
            }

            String tagName = tag.startsWith("#") ? tag.substring(1) : tag;
            String finalTagId = tagId != null ? tagId : UUID.randomUUID().toString();

            HashtagDocument hashtagDoc = new HashtagDocument();
            hashtagDoc.setId(finalTagId);
            hashtagDoc.setTag(tag.startsWith("#") ? tag : "#" + tag);
            hashtagDoc.setName(tagName);
            hashtagDoc.setPostCount(postCount);
            hashtagDoc.setCreatedAt(OffsetDateTime.now());
            hashtagDoc.setUpdatedAt(OffsetDateTime.now());

            indexingService.indexHashtag(hashtagDoc);
            logger.info("Indexed hashtag: {}", tag);

        } catch (Exception e) {
            logger.error("Error handling tag.created event: {}", e.getMessage(), e);
        }
    }

    /**
     * Extract hashtags from text.
     */
    private List<String> extractHashtags(String text) {
        if (text == null) {
            return List.of();
        }
        
        // Simple regex to find hashtags
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#\\w+");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        
        return matcher.results()
                .map(m -> m.group())
                .distinct()
                .toList();
    }

    /**
     * Index hashtag if it doesn't exist.
     */
    private void indexHashtagIfNew(String tag) {
        try {
            String tagName = tag.startsWith("#") ? tag.substring(1) : tag;
            String normalizedTag = tag.startsWith("#") ? tag : "#" + tag;
            
            // Check if already exists (simplified - in production, you'd check ES first)
            HashtagDocument hashtagDoc = new HashtagDocument();
            hashtagDoc.setId(UUID.randomUUID().toString());
            hashtagDoc.setTag(normalizedTag);
            hashtagDoc.setName(tagName);
            hashtagDoc.setPostCount(1); // Will be updated by aggregation
            hashtagDoc.setCreatedAt(OffsetDateTime.now());
            hashtagDoc.setUpdatedAt(OffsetDateTime.now());

            indexingService.indexHashtag(hashtagDoc);
        } catch (Exception e) {
            logger.error("Error indexing hashtag {}: {}", tag, e.getMessage());
        }
    }

    /**
     * Index location if it doesn't exist.
     */
    private void indexLocationIfNew(String locationName) {
        try {
            LocationDocument locationDoc = new LocationDocument();
            locationDoc.setId(UUID.randomUUID().toString());
            locationDoc.setName(locationName);
            locationDoc.setPostCount(1); // Will be updated by aggregation
            locationDoc.setCreatedAt(OffsetDateTime.now());
            locationDoc.setUpdatedAt(OffsetDateTime.now());

            indexingService.indexLocation(locationDoc);
        } catch (Exception e) {
            logger.error("Error indexing location {}: {}", locationName, e.getMessage());
        }
    }

    /**
     * Handle shop.created event.
     * Index the new shop in Elasticsearch.
     */
    @KafkaListener(topics = "shop.created", groupId = "discovery-service-group")
    public void handleShopCreated(Map<String, Object> event) {
        try {
            logger.info("Received shop.created event: {}", event);

            String shopId = (String) event.get("shopId");
            String businessAccountId = (String) event.get("businessAccountId");
            String name = (String) event.get("name");
            String description = (String) event.get("description");
            String category = (String) event.get("category");
            String profileImageUrl = (String) event.get("profileImageUrl");
            String coverImageUrl = (String) event.get("coverImageUrl");
            Boolean isActive = event.get("isActive") != null ? (Boolean) event.get("isActive") : true;
            Boolean isVerified = event.get("isVerified") != null ? (Boolean) event.get("isVerified") : false;
            Long productCount = event.get("productCount") != null ? ((Number) event.get("productCount")).longValue() : 0L;
            Long followerCount = event.get("followerCount") != null ? ((Number) event.get("followerCount")).longValue() : 0L;

            if (shopId == null) {
                logger.warn("Invalid shop.created event: missing shopId");
                return;
            }

            ShopDocument shopDoc = new ShopDocument();
            shopDoc.setId(shopId);
            shopDoc.setBusinessAccountId(businessAccountId);
            shopDoc.setName(name);
            shopDoc.setDescription(description);
            shopDoc.setCategory(category);
            shopDoc.setProfileImageUrl(profileImageUrl);
            shopDoc.setCoverImageUrl(coverImageUrl);
            shopDoc.setIsActive(isActive);
            shopDoc.setIsVerified(isVerified);
            shopDoc.setProductCount(productCount);
            shopDoc.setFollowerCount(followerCount);
            shopDoc.setCreatedAt(OffsetDateTime.now());
            shopDoc.setUpdatedAt(OffsetDateTime.now());

            indexingService.indexShop(shopDoc);
            logger.info("Indexed shop: {}", shopId);

        } catch (Exception e) {
            logger.error("Error handling shop.created event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle shop.updated event.
     * Update shop document in Elasticsearch.
     */
    @KafkaListener(topics = "shop.updated", groupId = "discovery-service-group")
    public void handleShopUpdated(Map<String, Object> event) {
        try {
            logger.info("Received shop.updated event: {}", event);

            String shopId = (String) event.get("shopId");
            if (shopId == null) {
                logger.warn("Invalid shop.updated event: missing shopId");
                return;
            }

            // Re-index the shop with updated data
            handleShopCreated(event); // Reuse creation logic

        } catch (Exception e) {
            logger.error("Error handling shop.updated event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle product.created event.
     * Index the new product in Elasticsearch.
     */
    @KafkaListener(topics = "product.created", groupId = "discovery-service-group")
    public void handleProductCreated(Map<String, Object> event) {
        try {
            logger.info("Received product.created event: {}", event);

            String productId = (String) event.get("productId");
            String shopId = (String) event.get("shopId");
            String name = (String) event.get("name");
            String description = (String) event.get("description");
            String category = (String) event.get("category");
            Double price = event.get("price") != null ? ((Number) event.get("price")).doubleValue() : null;
            String currency = (String) event.get("currency");
            String thumbnailUrl = (String) event.get("thumbnailUrl");
            Boolean isAvailable = event.get("isAvailable") != null ? (Boolean) event.get("isAvailable") : true;
            Boolean isFeatured = event.get("isFeatured") != null ? (Boolean) event.get("isFeatured") : false;
            Long viewCount = event.get("viewCount") != null ? ((Number) event.get("viewCount")).longValue() : 0L;
            Long likeCount = event.get("likeCount") != null ? ((Number) event.get("likeCount")).longValue() : 0L;

            if (productId == null) {
                logger.warn("Invalid product.created event: missing productId");
                return;
            }

            ProductDocument productDoc = new ProductDocument();
            productDoc.setId(productId);
            productDoc.setShopId(shopId);
            productDoc.setName(name);
            productDoc.setDescription(description);
            productDoc.setCategory(category);
            productDoc.setPrice(price);
            productDoc.setCurrency(currency != null ? currency : "USD");
            productDoc.setThumbnailUrl(thumbnailUrl);
            productDoc.setIsAvailable(isAvailable);
            productDoc.setIsFeatured(isFeatured);
            productDoc.setViewCount(viewCount);
            productDoc.setLikeCount(likeCount);
            productDoc.setCreatedAt(OffsetDateTime.now());
            productDoc.setUpdatedAt(OffsetDateTime.now());

            indexingService.indexProduct(productDoc);
            logger.info("Indexed product: {}", productId);

        } catch (Exception e) {
            logger.error("Error handling product.created event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle product.updated event.
     * Update product document in Elasticsearch.
     */
    @KafkaListener(topics = "product.updated", groupId = "discovery-service-group")
    public void handleProductUpdated(Map<String, Object> event) {
        try {
            logger.info("Received product.updated event: {}", event);

            String productId = (String) event.get("productId");
            if (productId == null) {
                logger.warn("Invalid product.updated event: missing productId");
                return;
            }

            // Re-index the product with updated data
            handleProductCreated(event); // Reuse creation logic

        } catch (Exception e) {
            logger.error("Error handling product.updated event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle product.deleted event.
     * Remove product from Elasticsearch index.
     */
    @KafkaListener(topics = "product.deleted", groupId = "discovery-service-group")
    public void handleProductDeleted(Map<String, Object> event) {
        try {
            logger.info("Received product.deleted event: {}", event);

            String productId = (String) event.get("productId");
            if (productId == null) {
                logger.warn("Invalid product.deleted event: missing productId");
                return;
            }

            indexingService.deleteProduct(productId);
            logger.info("Deleted product from index: {}", productId);

        } catch (Exception e) {
            logger.error("Error handling product.deleted event: {}", e.getMessage(), e);
        }
    }
}

