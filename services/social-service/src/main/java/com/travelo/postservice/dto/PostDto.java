package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelo.postservice.entity.Post;
import com.travelo.postservice.entity.PostMedia;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostDto {
    // Note: username and user_avatar are always included even if null to ensure UI has fallback values
    private String id;
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("username")
    @JsonInclude(JsonInclude.Include.ALWAYS)  // Always include username in response
    private String username;
    
    @JsonProperty("user_avatar")
    @JsonInclude(JsonInclude.Include.ALWAYS)  // Always include user_avatar in response
    private String userAvatar;
    
    @JsonProperty("post_type")
    private String postType;
    
    private String content; // Deprecated
    private List<String> images; // Deprecated
    
    @JsonProperty("media_items")
    private List<MediaItemDto> mediaItems;
    
    private String caption;
    private List<String> tags;
    private String mood;
    private String location;
    private Integer likes;
    private Integer comments;
    private Integer remixes;
    private Integer tips;
    private Integer shares;
    
    @JsonProperty("is_liked")
    private Boolean isLiked;
    
    @JsonProperty("is_following")
    private Boolean isFollowing;
    
    @JsonProperty("is_saved")
    private Boolean isSaved;

    /** In "Dream" collection (saved-posts table, collection_name = {@code Dream}). */
    @JsonProperty("is_dreamed")
    private Boolean isDreamed;
    
    @JsonProperty("is_verified")
    private Boolean isVerified;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    @JsonProperty("created_at")
    private String createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    @JsonProperty("updated_at")
    private String updatedAt;
    
    @JsonProperty("top_comment")
    private String topComment;
    
    @JsonProperty("top_comment_user")
    private String topCommentUser;
    
    private Integer duration;
    
    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;
    
    @JsonProperty("music_track")
    private String musicTrack;
    
    private Map<String, Object> metadata;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public String getPostType() {
        return postType;
    }

    public void setPostType(String postType) {
        this.postType = postType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public List<MediaItemDto> getMediaItems() {
        return mediaItems;
    }

    public void setMediaItems(List<MediaItemDto> mediaItems) {
        this.mediaItems = mediaItems;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getLikes() {
        return likes;
    }

    public void setLikes(Integer likes) {
        this.likes = likes;
    }

    public Integer getComments() {
        return comments;
    }

    public void setComments(Integer comments) {
        this.comments = comments;
    }

    public Integer getRemixes() {
        return remixes;
    }

    public void setRemixes(Integer remixes) {
        this.remixes = remixes;
    }

    public Integer getTips() {
        return tips;
    }

    public void setTips(Integer tips) {
        this.tips = tips;
    }

    public Integer getShares() {
        return shares;
    }

    public void setShares(Integer shares) {
        this.shares = shares;
    }

    public Boolean getIsLiked() {
        return isLiked;
    }

    public void setIsLiked(Boolean isLiked) {
        this.isLiked = isLiked;
    }

    public Boolean getIsFollowing() {
        return isFollowing;
    }

    public void setIsFollowing(Boolean isFollowing) {
        this.isFollowing = isFollowing;
    }

    public Boolean getIsSaved() {
        return isSaved;
    }

    public void setIsSaved(Boolean isSaved) {
        this.isSaved = isSaved;
    }

    public Boolean getIsDreamed() {
        return isDreamed;
    }

    public void setIsDreamed(Boolean isDreamed) {
        this.isDreamed = isDreamed;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTopComment() {
        return topComment;
    }

    public void setTopComment(String topComment) {
        this.topComment = topComment;
    }

    public String getTopCommentUser() {
        return topCommentUser;
    }

    public void setTopCommentUser(String topCommentUser) {
        this.topCommentUser = topCommentUser;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getMusicTrack() {
        return musicTrack;
    }

    public void setMusicTrack(String musicTrack) {
        this.musicTrack = musicTrack;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public static PostDto fromEntity(Post post) {
        return fromEntity(post, null);
    }
    
    public static PostDto fromEntity(Post post, com.travelo.postservice.client.MediaServiceClient mediaServiceClient) {
        PostDto dto = new PostDto();
        dto.setId(post.getId());
        dto.setUserId(post.getUserId());
        dto.setPostType(post.getPostType() != null ? post.getPostType().name().toLowerCase() : null);
        dto.setContent(post.getContent());
        dto.setCaption(post.getCaption());
        dto.setMood(post.getMood() != null ? post.getMood().name().toLowerCase() : null);
        dto.setLocation(post.getLocation());
        dto.setLikes(post.getLikes());
        dto.setComments(post.getComments());
        dto.setRemixes(post.getRemixes());
        dto.setTips(post.getTips());
        dto.setShares(post.getShares());
        dto.setDuration(post.getDuration());
        dto.setThumbnailUrl(post.getThumbnailUrl());
        dto.setMusicTrack(post.getMusicTrack());
        dto.setIsVerified(post.getIsVerified());
        
        if (post.getCreatedAt() != null) {
            dto.setCreatedAt(post.getCreatedAt().toString());
        }
        if (post.getUpdatedAt() != null) {
            dto.setUpdatedAt(post.getUpdatedAt().toString());
        }

        // Convert media items
        if (post.getMediaItems() != null && !post.getMediaItems().isEmpty()) {
            dto.setMediaItems(post.getMediaItems().stream()
                .map(item -> MediaItemDto.fromEntity(item, mediaServiceClient))
                .collect(Collectors.toList()));
        }

        // Convert legacy images
        if (post.getMedia() != null && !post.getMedia().isEmpty()) {
            dto.setImages(post.getMedia().stream()
                .map(PostMedia::getMediaUrl)
                .collect(Collectors.toList()));
        }

        // Convert tags
        if (post.getTags() != null && !post.getTags().isEmpty()) {
            dto.setTags(post.getTags().stream()
                .map(tag -> tag.getTag())
                .collect(Collectors.toList()));
        }

        return dto;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MediaItemDto {
        private String id;
        private String url;
        private String type;
        private Integer position;
        
        @JsonProperty("thumbnail_url")
        private String thumbnailUrl;
        
        private Integer duration;
        private Integer width;
        private Integer height;

        public static MediaItemDto fromEntity(com.travelo.postservice.entity.MediaItem item) {
            return fromEntity(item, null);
        }
        
        public static MediaItemDto fromEntity(com.travelo.postservice.entity.MediaItem item, 
                com.travelo.postservice.client.MediaServiceClient mediaServiceClient) {
            MediaItemDto dto = new MediaItemDto();
            dto.setId(item.getId() != null ? item.getId().toString() : null);
            dto.setType(item.getType() != null ? item.getType().name().toLowerCase() : null);
            dto.setPosition(item.getPosition());
            dto.setDuration(item.getDuration());
            dto.setWidth(item.getWidth());
            dto.setHeight(item.getHeight());
            
            // If mediaId exists, refresh URL from media service
            // Otherwise, use the stored URL (legacy format)
            if (item.getMediaId() != null && mediaServiceClient != null) {
                try {
                    // Try to get variants first (includes processed versions)
                    var variants = mediaServiceClient.getVariants(item.getMediaId(), true);
                    if (variants != null && variants.variants() != null && !variants.variants().isEmpty()) {
                        var firstVariant = variants.variants().get(0);
                        if (firstVariant.signedUrl() != null && !firstVariant.signedUrl().isEmpty()) {
                            dto.setUrl(firstVariant.signedUrl());
                        }
                        // Find thumbnail variant if available
                        var thumbVariant = variants.variants().stream()
                                .filter(v -> v.name() != null && v.name().contains("thumb"))
                                .findFirst()
                                .orElse(null);
                        if (thumbVariant != null && thumbVariant.signedUrl() != null && !thumbVariant.signedUrl().isEmpty()) {
                            dto.setThumbnailUrl(thumbVariant.signedUrl());
                        } else {
                            dto.setThumbnailUrl(item.getThumbnailUrl());
                        }
                    } else {
                        // Fallback to download URL
                        var downloadResponse = mediaServiceClient.getDownloadUrl(item.getMediaId(), null, 86400);
                        if (downloadResponse != null && downloadResponse.url() != null && !downloadResponse.url().isEmpty()) {
                            dto.setUrl(downloadResponse.url());
                        }
                        dto.setThumbnailUrl(item.getThumbnailUrl());
                    }
                    
                    // If URL is still not set after trying media service, fall back to stored URL
                    if (dto.getUrl() == null) {
                        dto.setUrl(item.getUrl());
                    }
                } catch (Exception e) {
                    // If fetching fails, use stored URL as fallback
                    org.slf4j.LoggerFactory.getLogger(MediaItemDto.class)
                        .debug("Failed to refresh URL from media service for mediaId={}, using stored URL", 
                            item.getMediaId(), e);
                    dto.setUrl(item.getUrl());
                    dto.setThumbnailUrl(item.getThumbnailUrl());
                }
            } else {
                // No mediaId or no client - use stored URL (legacy format)
                dto.setUrl(item.getUrl());
                dto.setThumbnailUrl(item.getThumbnailUrl());
            }
            
            return dto;
        }

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getPosition() {
            return position;
        }

        public void setPosition(Integer position) {
            this.position = position;
        }

        public String getThumbnailUrl() {
            return thumbnailUrl;
        }

        public void setThumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
        }

        public Integer getDuration() {
            return duration;
        }

        public void setDuration(Integer duration) {
            this.duration = duration;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }

        public Integer getHeight() {
            return height;
        }

        public void setHeight(Integer height) {
            this.height = height;
        }

        @Override
        public String toString() {
            return "MediaItemDto{" +
                    "id='" + id + '\'' +
                    ", url='" + url + '\'' +
                    ", type='" + type + '\'' +
                    ", position=" + position +
                    ", thumbnailUrl='" + thumbnailUrl + '\'' +
                    ", duration=" + duration +
                    ", width=" + width +
                    ", height=" + height +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "PostDto{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", userAvatar='" + userAvatar + '\'' +
                ", postType='" + postType + '\'' +
                ", content='" + content + '\'' +
                ", images=" + images +
                ", mediaItems=" + mediaItems +
                ", caption='" + caption + '\'' +
                ", tags=" + tags +
                ", mood='" + mood + '\'' +
                ", location='" + location + '\'' +
                ", likes=" + likes +
                ", comments=" + comments +
                ", remixes=" + remixes +
                ", tips=" + tips +
                ", shares=" + shares +
                ", isLiked=" + isLiked +
                ", isFollowing=" + isFollowing +
                ", isSaved=" + isSaved +
                ", isVerified=" + isVerified +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", topComment='" + topComment + '\'' +
                ", topCommentUser='" + topCommentUser + '\'' +
                ", duration=" + duration +
                ", thumbnailUrl='" + thumbnailUrl + '\'' +
                ", musicTrack='" + musicTrack + '\'' +
                ", metadata=" + metadata +
                '}';
    }
}

