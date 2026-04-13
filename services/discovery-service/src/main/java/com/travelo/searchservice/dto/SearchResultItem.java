package com.travelo.searchservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelo.searchservice.document.*;

import java.util.Map;

/**
 * Generic search result item that can represent users, hashtags, locations, or posts.
 */
public class SearchResultItem {
    
    @JsonProperty("type")
    private String type; // "user", "hashtag", "location", "post", "shop", "product"
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("subtitle")
    private String subtitle;
    
    @JsonProperty("imageUrl")
    private String imageUrl;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("relevanceScore")
    private Double relevanceScore;
    
    @JsonProperty("isFollowing")
    private Boolean isFollowing;

    public static SearchResultItem fromUser(UserDocument user) {
        SearchResultItem item = new SearchResultItem();
        item.setType("user");
        item.setId(user.getId());
        item.setTitle(user.getUsername());
        item.setSubtitle(user.getDisplayName());
        item.setImageUrl(user.getProfilePictureUrl() != null ? user.getProfilePictureUrl() : "");
        item.setMetadata(Map.of(
                "bio", user.getBio() != null ? user.getBio() : "",
                "isVerified", user.getIsVerified() != null ? user.getIsVerified() : false,
                "followerCount", user.getFollowerCount() != null ? user.getFollowerCount() : 0
        ));
        return item;
    }
    
    public static SearchResultItem fromUser(UserDocument user, Boolean isFollowing) {
        SearchResultItem item = fromUser(user);
        item.setIsFollowing(isFollowing);
        return item;
    }

    public static SearchResultItem fromHashtag(HashtagDocument hashtag) {
        SearchResultItem item = new SearchResultItem();
        item.setType("hashtag");
        item.setId(hashtag.getId());
        item.setTitle(hashtag.getTag());
        item.setSubtitle(hashtag.getPostCount() + " posts");
        item.setMetadata(Map.of(
                "postCount", hashtag.getPostCount() != null ? hashtag.getPostCount() : 0
        ));
        return item;
    }

    public static SearchResultItem fromLocation(LocationDocument location) {
        SearchResultItem item = new SearchResultItem();
        item.setType("location");
        item.setId(location.getId());
        item.setTitle(location.getName());
        item.setSubtitle(location.getCity() != null ? location.getCity() + ", " + location.getCountry() : location.getCountry());
        item.setMetadata(Map.of(
                "city", location.getCity() != null ? location.getCity() : "",
                "country", location.getCountry() != null ? location.getCountry() : "",
                "postCount", location.getPostCount() != null ? location.getPostCount() : 0
        ));
        return item;
    }

    public static SearchResultItem fromPost(PostDocument post) {
        if (post == null) {
            throw new IllegalArgumentException("PostDocument cannot be null");
        }
        
        SearchResultItem item = new SearchResultItem();
        item.setType("post");
        item.setId(post.getId() != null ? post.getId() : "");
        
        String caption = post.getCaption() != null ? post.getCaption() : "";
        item.setTitle(caption.length() > 50 ? caption.substring(0, 50) + "..." : caption);
        
        String username = post.getUsername() != null && !post.getUsername().isEmpty() 
                ? post.getUsername() 
                : "Unknown User";
        item.setSubtitle("by @" + username);
        item.setImageUrl(post.getThumbnailUrl() != null ? post.getThumbnailUrl() : "");
        
        // Enhanced metadata for reels/posts
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("userId", post.getUserId() != null ? post.getUserId() : "");
        metadata.put("username", username);
        metadata.put("caption", caption);
        metadata.put("likes", post.getLikes() != null ? post.getLikes() : 0);
        metadata.put("comments", post.getComments() != null ? post.getComments() : 0);
        metadata.put("shares", post.getShares() != null ? post.getShares() : 0);
        metadata.put("location", post.getLocation() != null ? post.getLocation() : "");
        metadata.put("tags", post.getTags() != null ? post.getTags() : java.util.Collections.emptyList());
        metadata.put("thumbnailUrl", post.getThumbnailUrl() != null ? post.getThumbnailUrl() : "");
        
        // For reels, mediaUrls should contain the video URL (first item)
        if (post.getMediaUrls() != null && !post.getMediaUrls().isEmpty()) {
            String firstMediaUrl = post.getMediaUrls().get(0);
            metadata.put("videoUrl", firstMediaUrl != null ? firstMediaUrl : "");
            metadata.put("mediaUrls", post.getMediaUrls());
        } else {
            metadata.put("videoUrl", "");
            metadata.put("mediaUrls", java.util.Collections.emptyList());
        }
        
        // Add createdAt for sorting
        if (post.getCreatedAt() != null) {
            metadata.put("createdAt", post.getCreatedAt().toString());
        }
        
        // User avatar - would need to fetch from user-service, but for now leave empty
        metadata.put("userAvatar", "");
        metadata.put("isLiked", false); // Would need to check from user-service
        
        item.setMetadata(metadata);
        return item;
    }

    public static SearchResultItem fromShop(ShopDocument shop) {
        SearchResultItem item = new SearchResultItem();
        item.setType("shop");
        item.setId(shop.getId());
        item.setTitle(shop.getName());
        item.setSubtitle(shop.getCategory() != null ? shop.getCategory() : "Shop");
        item.setImageUrl(shop.getProfileImageUrl() != null ? shop.getProfileImageUrl() : "");
        item.setMetadata(Map.of(
                "businessAccountId", shop.getBusinessAccountId() != null ? shop.getBusinessAccountId() : "",
                "description", shop.getDescription() != null ? shop.getDescription() : "",
                "category", shop.getCategory() != null ? shop.getCategory() : "",
                "isVerified", shop.getIsVerified() != null ? shop.getIsVerified() : false,
                "productCount", shop.getProductCount() != null ? shop.getProductCount() : 0L,
                "followerCount", shop.getFollowerCount() != null ? shop.getFollowerCount() : 0L
        ));
        return item;
    }

    public static SearchResultItem fromProduct(ProductDocument product) {
        SearchResultItem item = new SearchResultItem();
        item.setType("product");
        item.setId(product.getId());
        item.setTitle(product.getName());
        item.setSubtitle(product.getPrice() != null 
                ? String.format("%s %.2f", product.getCurrency() != null ? product.getCurrency() : "USD", product.getPrice())
                : "");
        item.setImageUrl(product.getThumbnailUrl() != null ? product.getThumbnailUrl() : "");
        item.setMetadata(Map.of(
                "shopId", product.getShopId() != null ? product.getShopId() : "",
                "description", product.getDescription() != null ? product.getDescription() : "",
                "category", product.getCategory() != null ? product.getCategory() : "",
                "price", product.getPrice() != null ? product.getPrice() : 0.0,
                "currency", product.getCurrency() != null ? product.getCurrency() : "USD",
                "isAvailable", product.getIsAvailable() != null ? product.getIsAvailable() : false,
                "isFeatured", product.getIsFeatured() != null ? product.getIsFeatured() : false,
                "viewCount", product.getViewCount() != null ? product.getViewCount() : 0L,
                "likeCount", product.getLikeCount() != null ? product.getLikeCount() : 0L
        ));
        return item;
    }

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public Double getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(Double relevanceScore) { this.relevanceScore = relevanceScore; }
    public Boolean getIsFollowing() { return isFollowing; }
    public void setIsFollowing(Boolean isFollowing) { this.isFollowing = isFollowing; }
}

