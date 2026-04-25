package com.travelo.postservice.service;

import com.travelo.postservice.client.dto.DirectUploadResponse;
import com.travelo.postservice.dto.*;

import java.util.List;
import java.util.UUID;

public interface PostService {

    /**
     * Create post with media IDs.
     * Use this after completing presigned uploads and getting media IDs.
     */
    PostDto createPost(String userId, CreatePostRequest request);

    /**
     * Direct upload file to media-service (local storage).
     */
    DirectUploadResponse uploadFile(org.springframework.web.multipart.MultipartFile file,
                                    UUID ownerId, String filename, String mediaType);

    /**
     * Request presigned upload URLs for files.
     * @deprecated Use uploadFile for local storage.
     */
    @Deprecated
    RequestUploadUrlResponse requestUploadUrl(RequestUploadUrlRequest request);

    /**
     * Confirm upload completion after client uploads file to S3.
     * @deprecated Use uploadFile for local storage.
     */
    @Deprecated
    CompleteUploadResponse completeUpload(UUID mediaId, CompleteUploadRequest request);

    /**
     * @param authorUserIds when non-null and non-empty, restrict to posts from these user ids (follow graph).
     * @param viewerUserId when set, enriches [is_liked, is_saved, is_dreamed] for that viewer.
     */
    PageResponse<PostDto> getPosts(int page, int limit, String mood, List<String> authorUserIds, String viewerUserId);

    default PageResponse<PostDto> getPosts(int page, int limit, String mood, List<String> authorUserIds) {
        return getPosts(page, limit, mood, authorUserIds, null);
    }

    default PageResponse<PostDto> getPosts(int page, int limit, String mood) {
        return getPosts(page, limit, mood, null, null);
    }

    default PostDto getPostById(String postId) {
        return getPostByIdForViewer(postId, null);
    }

    /**
     * @param viewerUserId optional; when set, enriches engagement fields for the viewer.
     */
    PostDto getPostByIdForViewer(String postId, String viewerUserId);

    PostDto updatePost(String postId, String userId, UpdatePostRequest request);

    void deletePost(String postId, String userId);

    PostDto likePost(String postId, String userId, boolean liked);

    PostDto unlikePost(String postId, String userId);

    PostDto sharePost(String postId);

    /**
     * Save or unsave a post (toggle).
     * If the post is already saved, it will be unsaved. If not saved, it will be saved.
     */
    PostDto savePost(String postId, String userId, SavePostRequest request);

    /**
     * List posts the given user has saved, most recent first.
     * Optionally filter by a specific collection name (null = all collections).
     */
    PageResponse<PostDto> listSavedPosts(String userId, int page, int limit, String collectionName);

    /**
     * Return the user's saved-post collections (name, size, cover image).
     */
    List<SavedCollectionDto> listSavedCollections(String userId);

    /**
     * List posts the given user has liked, most recent like first. Soft-deleted
     * posts (or posts the author has since taken down) are skipped so the
     * profile grid never shows a dangling tile.
     */
    PageResponse<PostDto> listLikedPosts(String userId, int page, int limit);

    /**
     * Paginated list of users who liked the post (newest like first), with display names from user-service when available.
     */
    PageResponse<PostLikeUserDto> listPostLikers(String postId, int page, int limit);

    long countPublishedPosts();
}

