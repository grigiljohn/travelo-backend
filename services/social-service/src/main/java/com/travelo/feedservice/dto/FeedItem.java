package com.travelo.feedservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelo.feedservice.client.dto.AdDeliveryResponse;
import com.travelo.feedservice.client.dto.PostDto;
import com.travelo.feedservice.client.dto.StoryPreviewDto;

import java.util.List;

/**
 * Unified feed item that can be post/reel/ad/story-cluster.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeedItem {
    
    @JsonProperty("type")
    private String type;  // "post" | "reel" | "ad" | "story_cluster"
    
    // Post fields (when type = "post")
    @JsonProperty("post_id")
    private String postId;
    
    @JsonProperty("post")
    private PostDto post;
    
    // Ad fields (when type = "ad")
    @JsonProperty("ad_id")
    private String adId;
    
    @JsonProperty("ad")
    private AdDeliveryResponse ad;

    // Story cluster fields (when type = "story_cluster")
    @JsonProperty("story_cluster_id")
    private String storyClusterId;

    @JsonProperty("stories")
    private List<StoryPreviewDto> stories;

    public FeedItem() {
    }

    public static FeedItem fromPost(PostDto post) {
        FeedItem item = new FeedItem();
        item.setType("post");
        item.setPostId(post.getId());
        item.setPost(post);
        return item;
    }

    public static FeedItem fromReel(PostDto post) {
        FeedItem item = new FeedItem();
        item.setType("reel");
        item.setPostId(post.getId());
        item.setPost(post);
        return item;
    }

    public static FeedItem fromAd(AdDeliveryResponse ad) {
        FeedItem item = new FeedItem();
        item.setType("ad");
        item.setAdId(ad.adId() != null ? ad.adId().toString() : null);
        item.setAd(ad);
        return item;
    }

    public static FeedItem storyCluster(String storyClusterId, List<StoryPreviewDto> stories) {
        FeedItem item = new FeedItem();
        item.setType("story_cluster");
        item.setStoryClusterId(storyClusterId);
        item.setStories(stories != null ? stories : List.of());
        return item;
    }

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public PostDto getPost() { return post; }
    public void setPost(PostDto post) { this.post = post; }
    public String getAdId() { return adId; }
    public void setAdId(String adId) { this.adId = adId; }
    public AdDeliveryResponse getAd() { return ad; }
    public void setAd(AdDeliveryResponse ad) { this.ad = ad; }
    public String getStoryClusterId() { return storyClusterId; }
    public void setStoryClusterId(String storyClusterId) { this.storyClusterId = storyClusterId; }
    public List<StoryPreviewDto> getStories() { return stories; }
    public void setStories(List<StoryPreviewDto> stories) { this.stories = stories; }
}

