package com.travelo.postservice.service.impl;

import com.travelo.postservice.client.MediaServiceClient;
import com.travelo.postservice.dto.*;
import com.travelo.postservice.entity.*;
import com.travelo.postservice.entity.enums.MediaType;
import com.travelo.postservice.entity.enums.MoodType;
import com.travelo.postservice.entity.enums.PostType;
import com.travelo.postservice.exception.PostNotFoundException;
import com.travelo.postservice.repository.*;
import com.travelo.postservice.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class PostServiceImpl implements PostService {

    private static final Logger logger = LoggerFactory.getLogger(PostServiceImpl.class);
    
    private final PostRepository postRepository;
    private final MediaItemRepository mediaItemRepository;
    private final PostTagRepository postTagRepository;
    private final LikeRepository likeRepository;
    private final SavedPostRepository savedPostRepository;
    private final MediaServiceClient mediaServiceClient;
    private final com.travelo.postservice.client.UserServiceClient userServiceClient;
    public PostServiceImpl(
            PostRepository postRepository,
            MediaItemRepository mediaItemRepository,
            PostTagRepository postTagRepository,
            LikeRepository likeRepository,
            SavedPostRepository savedPostRepository,
            MediaServiceClient mediaServiceClient,
            com.travelo.postservice.client.UserServiceClient userServiceClient) {
        this.postRepository = postRepository;
        this.mediaItemRepository = mediaItemRepository;
        this.postTagRepository = postTagRepository;
        this.likeRepository = likeRepository;
        this.savedPostRepository = savedPostRepository;
        this.mediaServiceClient = mediaServiceClient;
        this.userServiceClient = userServiceClient;
        logger.info("PostServiceImpl initialized");
    }

    @Override
    @Transactional
    public PostDto createPost(String userId, CreatePostRequest request) {
        logger.debug("Creating post - postType: {}, userId: {}", request.postType(), userId);
        // Determine post type from media items
        PostType postType = determinePostType(request);
        logger.debug("Determined post type: {}", postType);

        // Create post entity
        Post post = new Post();
        String postId = UUID.randomUUID().toString();
        post.setId(postId);
        post.setUserId(userId);
        post.setPostType(postType);
        logger.debug("Created post entity with ID: {}, userId: {}", postId, userId);

        // Handle backward compatibility: convert old format to new format
        List<MediaItemRequest> mediaItems = request.mediaItems();
        boolean isTextPost = "text".equalsIgnoreCase(request.postType());
        if (mediaItems == null || mediaItems.isEmpty()) {
            if (isTextPost) {
                // Text posts: do not treat content as media; keep mediaItems empty
                mediaItems = java.util.Collections.emptyList();
            } else {
                // Convert legacy format (content/images) to mediaItems
                mediaItems = convertLegacyToMediaItems(request);
            }
        }

        // Set content: for text posts use request content/caption; for media use first item URL
        if (isTextPost && (request.content() != null && !request.content().isBlank() || request.caption() != null && !request.caption().isBlank())) {
            String textContent = request.content() != null && !request.content().isBlank()
                ? request.content()
                : request.caption();
            post.setContent(textContent.length() > 500 ? textContent.substring(0, 500) : textContent);
            post.setCaption(textContent.length() > 1000 ? textContent.substring(0, 1000) : textContent);
        } else if (!mediaItems.isEmpty()) {
            MediaItemRequest firstItem = mediaItems.get(0);
            if (firstItem.usesMediaService() && firstItem.mediaId() != null) {
                try {
                    var mediaResponse = mediaServiceClient.getDownloadUrl(firstItem.mediaId(), null, 3600);
                    post.setContent(mediaResponse.url());
                } catch (Exception e) {
                    logger.warn("Failed to fetch media URL, using media ID as content", e);
                    post.setContent(firstItem.mediaId().toString());
                }
            } else {
                post.setContent(firstItem.url());
            }
        }

        if (!isTextPost) {
            post.setCaption(request.caption());
        }
        post.setMood(parseMood(request.mood()));
        post.setLocation(request.location());
        post.setMusicTrack(request.musicTrack());
        if (request.visibility() != null && !request.visibility().isBlank()) {
            try {
                String v = request.visibility().toUpperCase();
                if ("FRIENDS".equals(v)) v = "FOLLOWERS_ONLY";
                post.setPrivacyLevel(com.travelo.postservice.entity.enums.PrivacyLevel.valueOf(v));
            } catch (IllegalArgumentException e) {
                logger.debug("Invalid visibility '{}', using default PUBLIC", request.visibility());
            }
        }
        post.setLikes(0);
        post.setComments(0);
        post.setRemixes(0);
        post.setTips(0);
        post.setShares(0);
        post.setIsVerified(false); // TODO: Get from user service
        // createdAt and updatedAt are set by @PrePersist and @PreUpdate

        // Save post
        post = postRepository.save(post);
        logger.debug("Post saved to database - ID: {}", post.getId());

        // Save media items
        List<MediaItem> savedMediaItems = savePostMediaItems(post, mediaItems);
        post.setMediaItems(savedMediaItems);
        logger.debug("Saved {} media items for post ID: {}", savedMediaItems.size(), post.getId());

        // Save tags
        savePostTags(post, request.tags());
        logger.debug("Saved {} tags for post ID: {}", request.tags() != null ? request.tags().size() : 0, post.getId());

        PostDto dto = PostDto.fromEntity(post, mediaServiceClient);
        // Set username and userAvatar from user service
        populateUserInfo(dto, UUID.fromString(userId));
        dto.setIsLiked(false);
        dto.setIsFollowing(false);
        dto.setIsSaved(false);

        logger.info("Post created successfully - ID: {}, postType: {}, mood: {}", 
                post.getId(), postType, post.getMood());
        return dto;
    }

    @Override
    public com.travelo.postservice.client.dto.DirectUploadResponse uploadFile(
            org.springframework.web.multipart.MultipartFile file, UUID ownerId, String filename, String mediaType) {
        return mediaServiceClient.uploadFile(file, ownerId, filename, mediaType);
    }

    @Override
    @Deprecated
    public RequestUploadUrlResponse requestUploadUrl(RequestUploadUrlRequest request) {
        logger.info("Requesting upload URL - filename={}, size={}, mediaType={}", 
                request.filename(), request.sizeBytes(), request.mediaType());

        try {
            // Determine if resumable based on file size (default: > 10MB)
            boolean resumable = request.resumable() != null ? request.resumable() : 
                    (request.sizeBytes() > 10 * 1024 * 1024);

            // Map media type to media-service format
            String mediaType = mapMediaTypeToServiceFormat(request.mediaType());

            // Request upload URL from media-service
            var uploadResponse = mediaServiceClient.createUploadUrl(
                    request.ownerId(),
                    request.filename(),
                    request.mimeType(),
                    request.sizeBytes(),
                    mediaType,
                    resumable
            );

            logger.info("Upload URL generated - mediaId={}, method={}", 
                    uploadResponse.mediaId(), uploadResponse.uploadMethod());

            // Convert to response DTO
            return new RequestUploadUrlResponse(
                    uploadResponse.mediaId(),
                    uploadResponse.uploadMethod(),
                    uploadResponse.expiresIn(),
                    uploadResponse.uploadUrl(),
                    uploadResponse.storageKey(),
                    uploadResponse.uploadId(),
                    uploadResponse.partSize(),
                    uploadResponse.presignedPartUrls()
            );
        } catch (Exception e) {
            logger.error("Error requesting upload URL - filename={}", request.filename(), e);
            throw new RuntimeException("Failed to request upload URL: " + e.getMessage(), e);
        }
    }

    @Override
    public CompleteUploadResponse completeUpload(UUID mediaId, CompleteUploadRequest request) {
        logger.info("Completing upload - mediaId={}, sizeBytes={}", mediaId, request.sizeBytes());

        try {
            // Confirm upload with media-service
            mediaServiceClient.completeUpload(mediaId, request.etag(), request.sizeBytes());
            logger.debug("Upload confirmed with media-service - mediaId={}", mediaId);

            // Fetch the download URL for the uploaded media
            String downloadUrl = null;
            try {
                // Small delay to ensure media is available after upload completion
                Thread.sleep(500);
                
                // Try to get variants first (includes processed versions)
                try {
                    var variants = mediaServiceClient.getVariants(mediaId, true);
                    if (variants != null && variants.variants() != null && !variants.variants().isEmpty()) {
                        var firstVariant = variants.variants().get(0);
                        downloadUrl = firstVariant.signedUrl();
                    }
                } catch (Exception e) {
                    logger.debug("Failed to fetch variants, trying download URL", e);
                }
                
                // Fallback: get download URL for original file
                if (downloadUrl == null) {
                    var downloadResponse = mediaServiceClient.getDownloadUrl(mediaId, null, 86400); // 24 hours expiry
                    downloadUrl = downloadResponse.url();
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch download URL immediately after upload completion - mediaId={}. " +
                        "URL can be fetched later via media service.", mediaId, e);
            }

            logger.info("Upload completed successfully - mediaId={}", mediaId);
            return new CompleteUploadResponse(
                    mediaId,
                    "completed",
                    downloadUrl
            );
        } catch (Exception e) {
            logger.error("Error completing upload - mediaId={}", mediaId, e);
            throw new RuntimeException("Failed to complete upload: " + e.getMessage(), e);
        }
    }

    private String mapMediaTypeToServiceFormat(String mediaType) {
        if (mediaType == null) {
            return "OTHER";
        }
        return switch (mediaType.toLowerCase()) {
            case "image" -> "IMAGE";
            case "video" -> "VIDEO";
            case "audio" -> "AUDIO";
            default -> "OTHER";
        };
    }


    private PostType determinePostType(CreatePostRequest request) {
        logger.debug("Determining post type from request");
        List<MediaItemRequest> mediaItems = request.mediaItems();

        // If using legacy format, determine from post_type
        if (mediaItems == null || mediaItems.isEmpty()) {
            PostType type = PostType.valueOf(request.postType().toUpperCase());
            logger.debug("Using legacy format, post type: {}", type);
            return type;
        }

        // Determine from media items
        boolean hasImages = mediaItems.stream()
                .anyMatch(item -> "image".equalsIgnoreCase(item.type()));
        boolean hasVideos = mediaItems.stream()
                .anyMatch(item -> "video".equalsIgnoreCase(item.type()));

        PostType determinedType;
        if (hasImages && hasVideos) {
            determinedType = PostType.MIXED;
        } else if (hasVideos) {
            determinedType = PostType.VIDEO;
        } else {
            determinedType = PostType.IMAGE;
        }
        logger.debug("Determined post type from media items: {} (hasImages: {}, hasVideos: {})", 
                determinedType, hasImages, hasVideos);
        return determinedType;
    }

    private List<MediaItemRequest> convertLegacyToMediaItems(CreatePostRequest request) {
        List<MediaItemRequest> mediaItems = new ArrayList<>();

        // Convert content to media item
        if (request.content() != null && !request.content().isEmpty()) {
            MediaItemRequest item = new MediaItemRequest(
                    null, // mediaId
                    request.content(), // url
                    "image".equalsIgnoreCase(request.postType()) ? "image" : "video",
                    0,
                    null,
                    null,
                    null,
                    null
            );
            mediaItems.add(item);
        }

        // Convert images to media items
        if (request.images() != null) {
            for (int i = 0; i < request.images().size(); i++) {
                MediaItemRequest item = new MediaItemRequest(
                        null, // mediaId
                        request.images().get(i), // url
                        "image",
                        mediaItems.size(),
                        null,
                        null,
                        null,
                        null
                );
                mediaItems.add(item);
            }
        }

        return mediaItems;
    }

    private List<MediaItem> savePostMediaItems(Post post, List<MediaItemRequest> mediaItemRequests) {
        logger.debug("Saving {} media items for post ID: {}", mediaItemRequests.size(), post.getId());
        List<MediaItem> mediaItems = new ArrayList<>();

        for (int i = 0; i < mediaItemRequests.size(); i++) {
            MediaItemRequest request = mediaItemRequests.get(i);
            MediaItem mediaItem;

            if (request.usesMediaService() && request.mediaId() != null) {
                // New: Using media-service media ID
                String url = request.url(); // Use provided URL if available
                String thumbnailUrl = request.thumbnailUrl();
                com.travelo.postservice.client.dto.MediaFileResponse mediaMeta;

                try {
                    mediaMeta = mediaServiceClient.getMedia(request.mediaId());
                } catch (Exception e) {
                    logger.warn("Failed to fetch media metadata for mediaId={}, refusing attachment", request.mediaId(), e);
                    throw new IllegalArgumentException("Media not available for attachment: " + request.mediaId());
                }
                if (mediaMeta == null) {
                    throw new IllegalArgumentException("Media not found for attachment: " + request.mediaId());
                }
                ensureMediaAllowedForPost(mediaMeta, request.mediaId());

                // If URLs not provided, fetch from media-service
                if (url == null) {
                    try {
                        // Try to get variants first (includes processed versions)
                        var variants = mediaServiceClient.getVariants(request.mediaId(), true);
                        if (variants != null && variants.variants() != null && !variants.variants().isEmpty()) {
                            // Use first variant's signed URL
                            var firstVariant = variants.variants().get(0);
                            if (firstVariant.signedUrl() != null && !firstVariant.signedUrl().isEmpty()) {
                                url = firstVariant.signedUrl();
                            }
                            // Find thumbnail variant if available
                            var thumbVariant = variants.variants().stream()
                                    .filter(v -> v.name() != null && v.name().contains("thumb"))
                                    .findFirst()
                                    .orElse(null);
                            if (thumbVariant != null && thumbVariant.signedUrl() != null && !thumbVariant.signedUrl().isEmpty()) {
                                thumbnailUrl = thumbVariant.signedUrl();
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("Failed to fetch variants, trying download URL", e);
                    }
                    
                    // Fallback: generate download URL for original file (should always be available after upload)
                    if (url == null) {
                        // Retry logic with exponential backoff
                        int maxRetries = 3;
                        int retryDelayMs = 500;
                        boolean urlFetched = false;
                        
                        for (int retry = 0; retry < maxRetries && !urlFetched; retry++) {
                            if (retry > 0) {
                                try {
                                    Thread.sleep(retryDelayMs * retry); // Exponential backoff
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            }
                            
                            try {
                                var downloadResponse = mediaServiceClient.getDownloadUrl(request.mediaId(), null, 86400); // 24 hours expiry
                                if (downloadResponse != null && downloadResponse.url() != null && !downloadResponse.url().isEmpty()) {
                                    url = downloadResponse.url();
                                    urlFetched = true;
                                    logger.debug("Generated download URL for mediaId={} (attempt {})", request.mediaId(), retry + 1);
                                } else {
                                    logger.debug("Download URL response is null or empty for mediaId={} (attempt {})", 
                                            request.mediaId(), retry + 1);
                                }
                            } catch (Exception e) {
                                logger.debug("Failed to generate download URL for mediaId={} (attempt {})", 
                                        request.mediaId(), retry + 1, e);
                                if (retry == maxRetries - 1) {
                                    logger.warn("Failed to generate download URL for mediaId={} after {} attempts. " +
                                            "Proceeding without download URL; media can still be fetched later from media-service.",
                                            request.mediaId(), maxRetries);
                                }
                            }
                        }
                    }
                }

                mediaItem = new MediaItem(
                        post,
                        request.mediaId(),
                        url,
                        MediaType.valueOf(request.type().toUpperCase()),
                        request.position() != null ? request.position() : i
                );
                mediaItem.setThumbnailUrl(thumbnailUrl);
                if (request.width() == null) {
                    // Try to get from variants or meta
                    if (mediaMeta.variants() != null && !mediaMeta.variants().isEmpty()) {
                        var firstVariant = mediaMeta.variants().get(0);
                        mediaItem.setWidth(firstVariant.width());
                        mediaItem.setHeight(firstVariant.height());
                    }
                }
                if (request.duration() == null && mediaMeta.variants() != null) {
                    var videoVariant = mediaMeta.variants().stream()
                            .filter(v -> v.duration() != null)
                            .findFirst()
                            .orElse(null);
                    if (videoVariant != null) {
                        mediaItem.setDuration(videoVariant.duration().intValue());
                    }
                }
            } else {
                // Legacy: Using direct URL
                mediaItem = new MediaItem(
                        post,
                        request.url(),
                        MediaType.valueOf(request.type().toUpperCase()),
                        request.position() != null ? request.position() : i
                );
                mediaItem.setThumbnailUrl(request.thumbnailUrl());
            }

            // Set provided metadata if available
            if (request.duration() != null) {
                mediaItem.setDuration(request.duration());
            }
            if (request.width() != null) {
                mediaItem.setWidth(request.width());
            }
            if (request.height() != null) {
                mediaItem.setHeight(request.height());
            }

            mediaItem = mediaItemRepository.save(mediaItem);
            mediaItems.add(mediaItem);
            logger.debug("Saved media item {} for post ID: {} - mediaId={}, url={}, type: {}, position: {}", 
                    i + 1, post.getId(), mediaItem.getMediaId(), 
                    mediaItem.getUrl() != null ? "present" : "null", 
                    request.type(), mediaItem.getPosition());
            
            // Log warning if URL is still null after all attempts
            if (mediaItem.getUrl() == null && request.usesMediaService() && request.mediaId() != null) {
                logger.warn("Media item saved with null URL for mediaId={}, postId={}. " +
                        "URL should be fetched from media-service when reading the post.",
                        request.mediaId(), post.getId());
            }
        }

        logger.debug("Successfully saved {} media items for post ID: {}", mediaItems.size(), post.getId());
        return mediaItems;
    }

    private void ensureMediaAllowedForPost(com.travelo.postservice.client.dto.MediaFileResponse mediaMeta, UUID mediaId) {
        String safety = mediaMeta.safetyStatus() == null ? "unknown" : mediaMeta.safetyStatus().trim().toLowerCase(Locale.ROOT);
        String state = mediaMeta.state() == null ? "unknown" : mediaMeta.state().trim().toLowerCase(Locale.ROOT);
        if ("unsafe".equals(safety) || "review".equals(safety)) {
            throw new IllegalArgumentException("Media blocked by moderation policy: " + mediaId);
        }
        if (!"ready".equals(state)) {
            throw new IllegalArgumentException("Media is not ready for publishing: " + mediaId);
        }
    }

    private void savePostTags(Post post, List<String> tags) {
        if (tags != null && !tags.isEmpty()) {
            logger.debug("Saving {} tags for post ID: {}", tags.size(), post.getId());
            for (String tag : tags) {
                PostTag postTag = new PostTag(post, tag);
                postTagRepository.save(postTag);
            }
            logger.debug("Successfully saved {} tags for post ID: {}", tags.size(), post.getId());
        } else {
            logger.debug("No tags to save for post ID: {}", post.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostDto> getPosts(int page, int limit, String mood, List<String> authorUserIds) {
        logger.debug("Getting posts - page: {}, limit: {}, mood: {}, authorFilter={}",
                page, limit, mood, authorUserIds != null ? authorUserIds.size() : 0);

        long totalPosts = postRepository.count();
        long nonDeletedPosts = postRepository.countByDeletedAtIsNull();
        logger.debug("Database stats - Total posts: {}, Non-deleted posts: {}", totalPosts, nonDeletedPosts);

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());

        List<String> authorIds = authorUserIds == null ? List.of() : authorUserIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        Page<Post> postPage;
        if (!authorIds.isEmpty()) {
            if (mood != null && !mood.isEmpty()) {
                MoodType moodType = parseMood(mood);
                logger.debug("Filtering posts by followed authors (count={}) and mood {}", authorIds.size(), moodType);
                postPage = postRepository.findByUserIdInAndMoodAndDeletedAtIsNull(authorIds, moodType, pageable);
            } else {
                logger.debug("Filtering posts by followed authors (count={})", authorIds.size());
                postPage = postRepository.findByUserIdInAndDeletedAtIsNull(authorIds, pageable);
            }
        } else if (mood != null && !mood.isEmpty()) {
            MoodType moodType = parseMood(mood);
            logger.debug("Filtering posts by mood: {}", moodType);
            postPage = postRepository.findByMoodAndDeletedAtIsNull(moodType, pageable);
        } else {
            logger.debug("Getting all posts without mood filter");
            postPage = postRepository.findByDeletedAtIsNull(pageable);
        }

        logger.debug("Found {} posts (total: {}, pages: {})",
                postPage.getContent().size(), postPage.getTotalElements(), postPage.getTotalPages());

        List<PostDto> enrichedPosts = enrichPostsWithUserData(postPage.getContent());

        logger.info("Retrieved {} posts (page: {}, limit: {})",
                enrichedPosts.size(), page, limit);
        return PageResponse.<PostDto>builder()
                .data(enrichedPosts)
                .page(page)
                .limit(limit)
                .totalPosts(postPage.getTotalElements())
                .totalPages(postPage.getTotalPages())
                .hasNext(postPage.hasNext())
                .hasPrev(postPage.hasPrevious())
                .build();
    }

    private List<PostDto> enrichPostsWithUserData(List<Post> posts) {
        logger.debug("Enriching {} posts with user data", posts.size());
        return posts.stream()
                .map(post -> {
                    PostDto dto = PostDto.fromEntity(post, mediaServiceClient);
                    // Populate user info from user service
                    if (post.getUserId() != null && !post.getUserId().isEmpty()) {
                        try {
                            populateUserInfo(dto, UUID.fromString(post.getUserId()));
                            logger.debug("Enriched post {} with user info: username={}, userId={}", 
                                    post.getId(), dto.getUsername(), post.getUserId());
                        } catch (IllegalArgumentException e) {
                            logger.warn("Invalid userId format for post {}: {}", post.getId(), post.getUserId());
                            dto.setUsername("Unknown User");
                            dto.setUserAvatar("");
                        }
                    } else {
                        logger.warn("Post {} has null or empty userId", post.getId());
                        dto.setUsername("Unknown User");
                        dto.setUserAvatar("");
                    }
                    // Ensure username is never null
                    if (dto.getUsername() == null || dto.getUsername().isEmpty()) {
                        dto.setUsername("Unknown User");
                    }
                    // Set default values - will be implemented with authentication later
                    dto.setIsLiked(false);
                    dto.setIsFollowing(false);
                    dto.setIsSaved(false);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PostDto getPostById(String postId) {
        logger.debug("Getting post by ID: {}", postId);
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> {
                    logger.warn("Post not found - ID: {}", postId);
                    return new PostNotFoundException(postId);
                });

        PostDto dto = PostDto.fromEntity(post, mediaServiceClient);
        // Populate user info from user service
        if (post.getUserId() != null) {
            try {
                populateUserInfo(dto, UUID.fromString(post.getUserId()));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid userId format for post {}: {}", postId, post.getUserId());
                dto.setUsername("Unknown User");
                dto.setUserAvatar("");
            }
        } else {
            dto.setUsername("Unknown User");
            dto.setUserAvatar("");
        }
        // Set default values - will be implemented with authentication later
        dto.setIsLiked(false);
        dto.setIsFollowing(false);
        dto.setIsSaved(false);

        logger.info("Retrieved post - ID: {}", postId);
        return dto;
    }

    @Override
    @Transactional
    public PostDto updatePost(String postId, String userId, UpdatePostRequest request) {
        logger.debug("Updating post ID: {} by user: {}", postId, userId);
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> {
                    logger.warn("Post not found for update - ID: {}", postId);
                    return new PostNotFoundException(postId);
                });

        // P0 security fix: Verify user owns the post
        if (post.getUserId() == null || !post.getUserId().equals(userId)) {
            logger.warn("Unauthorized update attempt - postId: {}, postOwner: {}, requester: {}", 
                    postId, post.getUserId(), userId);
            throw new com.travelo.postservice.exception.UnauthorizedException(
                    "You do not have permission to update this post");
        }

        if (request.caption() != null) {
            logger.debug("Updating caption for post ID: {}", postId);
            post.setCaption(request.caption());
        }

        if (request.tags() != null) {
            logger.debug("Updating tags for post ID: {} - {} tags", postId, request.tags().size());
            // Delete existing tags
            postTagRepository.deleteByPostId(postId);
            // Save new tags
            savePostTags(post, request.tags());
        }

        if (request.location() != null) {
            logger.debug("Updating location for post ID: {}", postId);
            post.setLocation(request.location());
        }

        post.setUpdatedAt(OffsetDateTime.now());
        post = postRepository.save(post);
        logger.info("Post updated successfully - ID: {}", postId);

        PostDto dto = PostDto.fromEntity(post, mediaServiceClient);
        // Populate user info from user service
        if (post.getUserId() != null) {
            try {
                populateUserInfo(dto, UUID.fromString(post.getUserId()));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid userId format for post {}: {}", postId, post.getUserId());
                dto.setUsername("Unknown User");
                dto.setUserAvatar("");
            }
        } else {
            dto.setUsername("Unknown User");
            dto.setUserAvatar("");
        }
        dto.setIsLiked(false);
        dto.setIsFollowing(false);
        dto.setIsSaved(false);

        return dto;
    }

    @Override
    @Transactional
    public void deletePost(String postId, String userId) {
        logger.debug("Deleting post ID: {} by user: {}", postId, userId);
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> {
                    logger.warn("Post not found for deletion - ID: {}", postId);
                    return new PostNotFoundException(postId);
                });

        // P0 security fix: Verify user owns the post
        if (post.getUserId() == null || !post.getUserId().equals(userId)) {
            logger.warn("Unauthorized delete attempt - postId: {}, postOwner: {}, requester: {}", 
                    postId, post.getUserId(), userId);
            throw new com.travelo.postservice.exception.UnauthorizedException(
                    "You do not have permission to delete this post");
        }

        // Soft delete
        post.setDeletedAt(OffsetDateTime.now());
        postRepository.save(post);
        logger.info("Post soft deleted successfully - ID: {} by user: {}", postId, userId);
    }

    @Override
    @Transactional
    public PostDto likePost(String postId, String userId, boolean liked) {
        logger.debug("{} post ID: {} by user: {}", liked ? "Liking" : "Unliking", postId, userId);
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> {
                    logger.warn("Post not found for like/unlike - ID: {}", postId);
                    return new PostNotFoundException(postId);
                });

        boolean currentlyLiked = likeRepository.existsByUserIdAndPostId(userId, postId);
        logger.debug("Post ID: {} currently liked by user {}: {}", postId, userId, currentlyLiked);

        if (liked && !currentlyLiked) {
            // Add like
            Like like = new Like(userId, postId);
            likeRepository.save(like);
            post.setLikes(post.getLikes() + 1);
            logger.info("Like added - postId: {}, userId: {}, total likes: {}", postId, userId, post.getLikes());
        } else if (!liked && currentlyLiked) {
            // Remove like
            likeRepository.deleteByUserIdAndPostId(userId, postId);
            post.setLikes(Math.max(0, post.getLikes() - 1));
            logger.info("Like removed - postId: {}, userId: {}, total likes: {}", postId, userId, post.getLikes());
        } else {
            logger.debug("No change needed - postId: {}, userId: {}, liked: {}, currentlyLiked: {}", 
                    postId, userId, liked, currentlyLiked);
        }

        post = postRepository.save(post);

        PostDto dto = PostDto.fromEntity(post, mediaServiceClient);
        // Populate user info from user service
        if (post.getUserId() != null) {
            try {
                populateUserInfo(dto, UUID.fromString(post.getUserId()));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid userId format for post {}: {}", postId, post.getUserId());
                dto.setUsername("Unknown User");
                dto.setUserAvatar("");
            }
        } else {
            dto.setUsername("Unknown User");
            dto.setUserAvatar("");
        }
        dto.setIsLiked(liked);
        dto.setIsFollowing(false);
        dto.setIsSaved(false);

        return dto;
    }

    @Override
    @Transactional
    public PostDto unlikePost(String postId, String userId) {
        return likePost(postId, userId, false);
    }

    @Override
    @Transactional
    public PostDto sharePost(String postId) {
        logger.debug("Sharing post ID: {}", postId);
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> {
                    logger.warn("Post not found for share - ID: {}", postId);
                    return new PostNotFoundException(postId);
                });

        int previousShares = post.getShares();
        post.setShares(post.getShares() + 1);
        post = postRepository.save(post);
        logger.info("Post shared - postId: {}, shares: {} -> {}", 
                postId, previousShares, post.getShares());

        PostDto dto = PostDto.fromEntity(post, mediaServiceClient);
        // Populate user info from user service
        if (post.getUserId() != null) {
            try {
                populateUserInfo(dto, UUID.fromString(post.getUserId()));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid userId format for post {}: {}", postId, post.getUserId());
                dto.setUsername("Unknown User");
                dto.setUserAvatar("");
            }
        } else {
            dto.setUsername("Unknown User");
            dto.setUserAvatar("");
        }
        dto.setIsLiked(false);
        dto.setIsFollowing(false);
        dto.setIsSaved(false);

        return dto;
    }

    @Override
    @Transactional
    public PostDto savePost(String postId, String userId, SavePostRequest request) {
        logger.debug("Saving/unsaving post ID: {} by user: {}, collectionName: {}", postId, userId,
                request != null ? request.collectionName() : null);
        
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> {
                    logger.warn("Post not found for save/unsave - ID: {}", postId);
                    return new PostNotFoundException(postId);
                });

        String collectionName = request != null && request.collectionName() != null 
                ? request.collectionName() 
                : "All Posts";

        // Check if post is already saved
        boolean isSaved = savedPostRepository.existsByUserIdAndPostId(userId, postId);
        
        if (isSaved) {
            // Unsave: Delete the saved post entry
            savedPostRepository.findByUserIdAndPostIdAndCollectionName(userId, postId, collectionName)
                    .ifPresentOrElse(
                            savedPost -> {
                                savedPostRepository.delete(savedPost);
                                logger.info("Post unsaved - postId: {}, userId: {}, collectionName: {}", 
                                        postId, userId, collectionName);
                            },
                            () -> {
                                // If not found in specific collection, try to delete from any collection
                                savedPostRepository.findByUserIdAndPostIdAndCollectionName(userId, postId, "All Posts")
                                        .ifPresent(savedPost -> {
                                            savedPostRepository.delete(savedPost);
                                            logger.info("Post unsaved from default collection - postId: {}, userId: {}", 
                                                    postId, userId);
                                        });
                            }
                    );
        } else {
            // Save: Create new saved post entry
            SavedPost savedPost = new SavedPost(userId, postId, collectionName);
            savedPostRepository.save(savedPost);
            logger.info("Post saved - postId: {}, userId: {}, collectionName: {}", 
                    postId, userId, collectionName);
        }

        PostDto dto = PostDto.fromEntity(post, mediaServiceClient);
        // Populate user info from user service
        if (post.getUserId() != null) {
            try {
                populateUserInfo(dto, UUID.fromString(post.getUserId()));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid userId format for post {}: {}", postId, post.getUserId());
                dto.setUsername("Unknown User");
                dto.setUserAvatar("");
            }
        } else {
            dto.setUsername("Unknown User");
            dto.setUserAvatar("");
        }
        dto.setIsLiked(false);
        dto.setIsFollowing(false);
        dto.setIsSaved(!isSaved); // Toggle: if it was saved, now it's unsaved (false), and vice versa

        return dto;
    }

    /**
     * Helper method to populate username and userAvatar in PostDto from user-service.
     * Gracefully handles failures by leaving fields as null (will show as "Unknown User" in UI).
     */
    private void populateUserInfo(PostDto dto, UUID userId) {
        try {
            logger.info("Populating user info for userId={}", userId);
            
            // Check if userServiceClient is available
            if (userServiceClient == null) {
                logger.error("UserServiceClient is null! Cannot fetch user info for userId={}", userId);
                dto.setUsername("Unknown User");
                dto.setUserAvatar("");
                return;
            }
            
            com.travelo.postservice.client.dto.UserDto user = userServiceClient.getUser(userId);
            if (user != null && user.getUsername() != null && !user.getUsername().isEmpty()) {
                dto.setUsername(user.getUsername());
                dto.setUserAvatar(user.getProfilePictureUrl() != null ? user.getProfilePictureUrl() : "");
                logger.info("Successfully populated user info for userId={}: username={}, avatar={}", 
                        userId, user.getUsername(), user.getProfilePictureUrl());
            } else {
                logger.error("UserService returned null or empty username for userId={}. User object: {}, using default values", 
                        userId, user != null ? "username=" + user.getUsername() : "null", userId);
                dto.setUsername("Unknown User");
                dto.setUserAvatar("");
            }
        } catch (Exception e) {
            logger.error("Failed to fetch user info for userId={}, using default values. Error type: {}, message: {}", 
                    userId, e.getClass().getSimpleName(), e.getMessage(), e);
            dto.setUsername("Unknown User");
            dto.setUserAvatar("");
        }
    }

    private static MoodType parseMood(String raw) {
        if (raw == null || raw.isBlank()) {
            return MoodType.NEUTRAL;
        }
        String key = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return MoodType.valueOf(key);
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown mood '{}', using NEUTRAL", raw);
            return MoodType.NEUTRAL;
        }
    }
}
