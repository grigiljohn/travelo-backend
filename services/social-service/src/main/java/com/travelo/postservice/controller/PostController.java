package com.travelo.postservice.controller;

import com.travelo.commons.idempotency.IdempotencyKey;
import com.travelo.commons.security.SecurityUtils;
import com.travelo.postservice.dto.*;
import com.travelo.postservice.service.PostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private static final Logger logger = LoggerFactory.getLogger(PostController.class);
    private final PostService postService;
    private final boolean devHeaderUserFallbackEnabled;

    public PostController(
            PostService postService,
            @Value("${app.dev.allow-header-user-fallback:true}") boolean devHeaderUserFallbackEnabled) {
        this.postService = postService;
        this.devHeaderUserFallbackEnabled = devHeaderUserFallbackEnabled;
        logger.info("PostController initialized");
    }

    /**
     * Direct upload a single file to local storage.
     * POST /api/v1/posts/upload (multipart: file, owner_id from JWT)
     */
    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<com.travelo.postservice.client.dto.DirectUploadResponse>> uploadFile(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String ownerId = SecurityUtils.getCurrentUserIdAsString();
        if (ownerId == null) {
            throw new com.travelo.postservice.exception.UnauthorizedException("User not authenticated");
        }
        String mediaType = file.getContentType() != null && file.getContentType().startsWith("video/") ? "video" : "image";
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        var response = postService.uploadFile(file, UUID.fromString(ownerId), filename, mediaType);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("File uploaded successfully", response));
    }

    /**
     * Request presigned upload URLs for files.
     * @deprecated Use POST /upload for direct upload (local storage).
     */
    @Deprecated
    @PostMapping("/upload-urls")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<ApiResponse<RequestUploadUrlResponse>> requestUploadUrl(
            @Valid @RequestBody RequestUploadUrlRequest request) {
        logger.info("Requesting upload URL - filename={}, size={}, mediaType={}", 
                request.filename(), request.sizeBytes(), request.mediaType());
        try {
            RequestUploadUrlResponse response = postService.requestUploadUrl(request);
            logger.info("Upload URL generated - mediaId={}", response.mediaId());
            return ResponseEntity.ok(ApiResponse.success("Upload URL generated successfully", response));
        } catch (Exception e) {
            logger.error("Error requesting upload URL", e);
            throw e;
        }
    }

    /**
     * Confirm upload completion after client uploads file to S3.
     * Returns the download URL for the uploaded media.
     */
    @PostMapping("/upload/complete/{mediaId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<ApiResponse<CompleteUploadResponse>> completeUpload(
            @PathVariable("mediaId") UUID mediaId,
            @Valid @RequestBody CompleteUploadRequest request) {
        logger.info("Completing upload - mediaId={}", mediaId);
        try {
            CompleteUploadResponse response = postService.completeUpload(mediaId, request);
            logger.info("Upload completed - mediaId={}", mediaId);
            return ResponseEntity.ok(ApiResponse.success("Upload completed successfully", response));
        } catch (Exception e) {
            logger.error("Error completing upload - mediaId={}", mediaId, e);
            throw e;
        }
    }

    /**
     * Create post with multipart file uploads.
     * Files are uploaded directly to S3 using presigned URLs, then post is created.
     */
    @PostMapping(consumes = {"multipart/form-data"})
    @IdempotencyKey(ttlSeconds = 3600, required = false)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<PostDto>> createPostWithFiles(
            @RequestPart("files") MultipartFile[] files,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam("mood") String mood,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "tags", required = false) String tags) {
        // Extract user ID from JWT token (P0 security fix - remove insecure owner_id parameter)
        String ownerId = SecurityUtils.getCurrentUserIdAsString();
        if (ownerId == null) {
            logger.warn("Unauthenticated attempt to create post with files");
            throw new com.travelo.postservice.exception.UnauthorizedException("User not authenticated");
        }
        logger.info("Creating post with multipart files - fileCount: {}, mood: {}", files.length, mood);
        try {
            // Upload files directly to media-service (local storage)
            List<MediaItemRequest> mediaItems = new ArrayList<>();
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                if (file.isEmpty()) {
                    continue;
                }
                
                logger.debug("Processing file {}: {} ({} bytes)", i, file.getOriginalFilename(), file.getSize());
                
                String mediaType = file.getContentType() != null && file.getContentType().startsWith("video/") 
                    ? "video" : "image";
                String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
                
                // Direct upload to media-service (local storage)
                var uploadResponse = postService.uploadFile(file, UUID.fromString(ownerId), filename, mediaType);
                logger.debug("Uploaded file {}: mediaId={}, downloadUrl={}", 
                    i, uploadResponse.mediaId(), uploadResponse.downloadUrl() != null ? "present" : "null");
                
                // Create media item request from direct upload response
                MediaItemRequest mediaItem = new MediaItemRequest(
                    uploadResponse.mediaId(),
                    uploadResponse.downloadUrl(),
                    mediaType,
                    i,
                    null, null, null, null
                );
                mediaItems.add(mediaItem);
            }
            
            // Parse tags if provided
            List<String> tagList = null;
            if (tags != null && !tags.trim().isEmpty()) {
                tagList = Arrays.asList(tags.split(","));
            }
            
            // Determine post type from media items
            String postType = determinePostTypeFromMediaItems(mediaItems);
            
            // Step 5: Create post
            CreatePostRequest createRequest = new CreatePostRequest(
                postType,
                null, // content (deprecated)
                null, // images (deprecated)
                mediaItems,
                caption,
                tagList,
                mood,
                location,
                null, // musicTrack
                null  // visibility
            );
            
            // User ID already extracted from JWT token above (P0 security fix)
            PostDto post = postService.createPost(ownerId, createRequest);
            logger.info("Post created successfully with ID: {} from multipart upload, userId: {}", post.getId(), ownerId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Post created successfully", post));
        } catch (Exception e) {
            logger.error("Error creating post with multipart files", e);
            throw e;
        }
    }

    /**
     * Create post with media IDs.
     * Use this endpoint after completing presigned uploads and getting media IDs.
     * Requires authentication - user ID is extracted from JWT token.
     */
    @PostMapping(consumes = {"application/json"})
    @IdempotencyKey(ttlSeconds = 3600, required = false)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<PostDto>> createPost(
            @Valid @RequestBody CreatePostRequest request) {
        // Extract user ID from JWT token (P0 security fix)
        String userId = SecurityUtils.getCurrentUserIdAsString();
        if (userId == null) {
            logger.warn("Unauthenticated attempt to create post");
            throw new com.travelo.postservice.exception.UnauthorizedException("User not authenticated");
        }
        
        logger.info("Creating post - postType: {}, mood: {}, userId: {}", request.postType(), request.mood(), userId);
        logger.debug("Create post request: {}", request);
        try {
            PostDto post = postService.createPost(userId, request);
            logger.info("Post created successfully with ID: {}, userId: {}", post.getId(), userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Post created successfully", post));
        } catch (Exception e) {
            logger.error("Error creating post for user: {}", userId, e);
            throw e;
        }
    }
    
    /**
     * Determine post type from media items.
     */
    private String determinePostTypeFromMediaItems(List<MediaItemRequest> mediaItems) {
        boolean hasImages = mediaItems.stream().anyMatch(item -> "image".equalsIgnoreCase(item.type()));
        boolean hasVideos = mediaItems.stream().anyMatch(item -> "video".equalsIgnoreCase(item.type()));
        
        if (hasImages && hasVideos) {
            return "mixed";
        } else if (hasVideos) {
            return "video";
        } else {
            return "image";
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PostDto>>> getPosts(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") @Max(200) int limit,
            @RequestParam(value = "mood", required = false) String mood,
            @RequestParam(value = "author_ids", required = false) List<String> authorIds) {
        logger.debug("Getting posts - page: {}, limit: {}, mood: {}, authorIds={}", page, limit, mood,
                authorIds == null ? 0 : authorIds.size());
        try {
            PageResponse<PostDto> posts = postService.getPosts(page, limit, mood, authorIds);
            logger.info("Retrieved {} posts (page: {}, limit: {})", 
                    posts.getData() != null ? posts.getData().size() : 0, page, limit);
            return ResponseEntity.ok(ApiResponse.success("Posts retrieved successfully", posts));
        } catch (Exception e) {
            logger.error("Error retrieving posts", e);
            throw e;
        }
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDto>> getPostById(
            @PathVariable String postId) {
        logger.debug("Getting post by ID: {}", postId);
        try {
            PostDto post = postService.getPostById(postId);
            logger.info("Post retrieved successfully - ID: {}", postId);
            return ResponseEntity.ok(ApiResponse.success("Post retrieved successfully", post));
        } catch (Exception e) {
            logger.error("Error retrieving post with ID: {}", postId, e);
            throw e;
        }
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDto>> updatePost(
            @PathVariable String postId,
            @Valid @RequestBody UpdatePostRequest request) {
        // Extract user ID from JWT token and verify authorization (P0 security fix)
        String userId = SecurityUtils.getCurrentUserIdAsString();
        if (userId == null) {
            logger.warn("Unauthenticated attempt to update post: {}", postId);
            throw new com.travelo.postservice.exception.UnauthorizedException("User not authenticated");
        }
        
        logger.info("Updating post ID: {} by user: {}", postId, userId);
        logger.debug("Update post request: {}", request);
        try {
            PostDto post = postService.updatePost(postId, userId, request);
            logger.info("Post updated successfully - ID: {}", postId);
            return ResponseEntity.ok(ApiResponse.success("Post updated successfully", post));
        } catch (Exception e) {
            logger.error("Error updating post ID: {} by user: {}", postId, userId, e);
            throw e;
        }
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable String postId) {
        // Extract user ID from JWT token and verify authorization (P0 security fix)
        String userId = SecurityUtils.getCurrentUserIdAsString();
        if (userId == null) {
            logger.warn("Unauthenticated attempt to delete post: {}", postId);
            throw new com.travelo.postservice.exception.UnauthorizedException("User not authenticated");
        }
        
        logger.info("Deleting post ID: {} by user: {}", postId, userId);
        try {
            postService.deletePost(postId, userId);
            logger.info("Post deleted successfully - ID: {}", postId);
            return ResponseEntity.ok(ApiResponse.success("Post deleted successfully", null));
        } catch (Exception e) {
            logger.error("Error deleting post ID: {} by user: {}", postId, userId, e);
            throw e;
        }
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<PostDto>> likePost(
            @PathVariable String postId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestBody(required = false) LikeRequest request) {
        String userId = resolveUserIdOrThrow(headerUserId, "like", postId);
        if (userId == null) {
            logger.warn("Unauthenticated attempt to like post: {}", postId);
            throw new com.travelo.postservice.exception.UnauthorizedException("User not authenticated");
        }
        
        boolean liked = request != null && request.liked() != null ? request.liked() : true;
        logger.info("{} post ID: {} by user: {}", liked ? "Liking" : "Unliking", postId, userId);
        try {
            PostDto post = postService.likePost(postId, userId, liked);
            String message = liked ? "Post liked successfully" : "Post unliked successfully";
            logger.info("Post {} successfully - ID: {}, userId: {}, total likes: {}", 
                    liked ? "liked" : "unliked", postId, userId, post.getLikes());
            return ResponseEntity.ok(ApiResponse.success(message, post));
        } catch (Exception e) {
            logger.error("Error {} post ID: {} by user: {}", liked ? "liking" : "unliking", postId, userId, e);
            throw e;
        }
    }

    @DeleteMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<PostDto>> unlikePost(
            @PathVariable String postId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        String userId = resolveUserIdOrThrow(headerUserId, "unlike", postId);
        if (userId == null) {
            logger.warn("Unauthenticated attempt to unlike post: {}", postId);
            throw new com.travelo.postservice.exception.UnauthorizedException("User not authenticated");
        }
        
        logger.info("Unliking post ID: {} by user: {}", postId, userId);
        try {
            PostDto post = postService.unlikePost(postId, userId);
            logger.info("Post unliked successfully - ID: {}, userId: {}, total likes: {}", postId, userId, post.getLikes());
            return ResponseEntity.ok(ApiResponse.success("Post unliked successfully", post));
        } catch (Exception e) {
            logger.error("Error unliking post ID: {} by user: {}", postId, userId, e);
            throw e;
        }
    }

    @PostMapping("/{postId}/share")
    public ResponseEntity<ApiResponse<ShareResponse>> sharePost(
            @PathVariable String postId,
            @RequestBody(required = false) ShareRequest request) {
        String platform = request != null ? request.platform() : "unknown";
        logger.info("Sharing post ID: {} on platform: {}", postId, platform);
        try {
            PostDto post = postService.sharePost(postId);
            ShareResponse shareResponse = new ShareResponse(
                    post.getShares(),
                    "https://your-app.com/posts/" + postId
            );
            logger.info("Post shared successfully - ID: {}, total shares: {}", postId, post.getShares());
            return ResponseEntity.ok(ApiResponse.success("Post shared successfully", shareResponse));
        } catch (Exception e) {
            logger.error("Error sharing post ID: {}", postId, e);
            throw e;
        }
    }

    /**
     * Save or unsave a post (toggle).
     * If the post is already saved, it will be unsaved. If not saved, it will be saved.
     */
    @PostMapping("/{postId}/save")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<ApiResponse<PostDto>> savePost(
            @PathVariable String postId,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestBody(required = false) SavePostRequest request) {
        String userId = resolveUserIdOrThrow(headerUserId, "save", postId);
        if (userId == null) {
            logger.warn("Unauthenticated attempt to save/unsave post: {}", postId);
            throw new com.travelo.postservice.exception.UnauthorizedException("User not authenticated");
        }
        logger.info("Save/unsave post request - postId: {}, userId: {}, requestBody: {}", postId, userId, request);
        logger.debug("Save post request details - postId: {}, userId: {}, collectionName: {}", 
                postId, userId, request != null ? request.collectionName() : null);
        try {
            PostDto post = postService.savePost(postId, userId, request);
            boolean isSaved = post.getIsSaved() != null && post.getIsSaved();
            String action = isSaved ? "saved" : "unsaved";
            logger.info("Post {} successfully - ID: {}, userId: {}, isSaved: {}", action, postId, userId, isSaved);
            return ResponseEntity.ok(ApiResponse.success("Post " + action + " successfully", post));
        } catch (Exception e) {
            logger.error("Error saving/unsaving post ID: {} by user: {}, requestBody: {}", postId, userId, request, e);
            throw e;
        }
    }

    // Inner classes for request/response
    public record LikeRequest(Boolean liked) {
    }

    public record ShareRequest(String platform) {
    }

    public record ShareResponse(Integer shares, String shareUrl) {
    }

    private String resolveUserIdOrThrow(String headerUserId, String action, String postId) {
        String userId = SecurityUtils.getCurrentUserIdAsString();
        if (userId != null) {
            return userId;
        }
        if (devHeaderUserFallbackEnabled && headerUserId != null && !headerUserId.isBlank()) {
            logger.warn(
                    "Using X-User-Id fallback for {} on post {} (dev mode only): {}",
                    action,
                    postId,
                    headerUserId
            );
            return headerUserId;
        }
        return null;
    }
}
