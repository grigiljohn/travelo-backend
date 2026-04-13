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
     */
    PageResponse<PostDto> getPosts(int page, int limit, String mood, List<String> authorUserIds);

    default PageResponse<PostDto> getPosts(int page, int limit, String mood) {
        return getPosts(page, limit, mood, null);
    }

    PostDto getPostById(String postId);

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
}

