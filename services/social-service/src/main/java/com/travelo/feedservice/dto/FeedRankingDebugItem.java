package com.travelo.feedservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Per-item ranking explainability payload.
 */
public class FeedRankingDebugItem {

    @JsonProperty("post_id")
    private String postId;

    @JsonProperty("author_user_id")
    private String authorUserId;

    @JsonProperty("post_type")
    private String postType;

    @JsonProperty("following_author")
    private boolean followingAuthor;

    @JsonProperty("base_score")
    private double baseScore;

    @JsonProperty("online_signal_score")
    private double onlineSignalScore;

    @JsonProperty("final_score")
    private double finalScore;

    @JsonProperty("recency_component")
    private double recencyComponent;

    @JsonProperty("affinity_component")
    private double affinityComponent;

    @JsonProperty("popularity_component")
    private double popularityComponent;

    @JsonProperty("content_type_component")
    private double contentTypeComponent;

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getAuthorUserId() {
        return authorUserId;
    }

    public void setAuthorUserId(String authorUserId) {
        this.authorUserId = authorUserId;
    }

    public String getPostType() {
        return postType;
    }

    public void setPostType(String postType) {
        this.postType = postType;
    }

    public boolean isFollowingAuthor() {
        return followingAuthor;
    }

    public void setFollowingAuthor(boolean followingAuthor) {
        this.followingAuthor = followingAuthor;
    }

    public double getBaseScore() {
        return baseScore;
    }

    public void setBaseScore(double baseScore) {
        this.baseScore = baseScore;
    }

    public double getOnlineSignalScore() {
        return onlineSignalScore;
    }

    public void setOnlineSignalScore(double onlineSignalScore) {
        this.onlineSignalScore = onlineSignalScore;
    }

    public double getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(double finalScore) {
        this.finalScore = finalScore;
    }

    public double getRecencyComponent() {
        return recencyComponent;
    }

    public void setRecencyComponent(double recencyComponent) {
        this.recencyComponent = recencyComponent;
    }

    public double getAffinityComponent() {
        return affinityComponent;
    }

    public void setAffinityComponent(double affinityComponent) {
        this.affinityComponent = affinityComponent;
    }

    public double getPopularityComponent() {
        return popularityComponent;
    }

    public void setPopularityComponent(double popularityComponent) {
        this.popularityComponent = popularityComponent;
    }

    public double getContentTypeComponent() {
        return contentTypeComponent;
    }

    public void setContentTypeComponent(double contentTypeComponent) {
        this.contentTypeComponent = contentTypeComponent;
    }
}

