package com.travelo.postservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Post draft entity - stores unpublished post drafts.
 */
@Entity
@Table(name = "post_drafts", indexes = {
    @Index(name = "idx_post_drafts_user_id", columnList = "user_id"),
    @Index(name = "idx_post_drafts_created_at", columnList = "created_at"),
    @Index(name = "idx_post_drafts_updated_at", columnList = "updated_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "media_file_path", length = 500)
    private String mediaFilePath;

    @Column(name = "is_video", nullable = false)
    @Builder.Default
    private Boolean isVideo = false;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "caption", length = 2000)
    private String caption;

    @Column(name = "text", length = 2000)
    private String text;

    @Column(name = "hashtags", length = 1000)
    private String hashtags; // JSON array as string

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "tagged_users", length = 1000)
    private String taggedUsers; // JSON array as string

    @Column(name = "audience", length = 50)
    private String audience; // public, followers, private

    @Column(name = "allow_comments", nullable = false)
    @Builder.Default
    private Boolean allowComments = true;

    @Column(name = "hide_likes_count", nullable = false)
    @Builder.Default
    private Boolean hideLikesCount = false;

    @Column(name = "allow_remixing", nullable = false)
    @Builder.Default
    private Boolean allowRemixing = true;

    @Column(name = "ai_label_enabled", nullable = false)
    @Builder.Default
    private Boolean aiLabelEnabled = false;

    @Column(name = "music_track_id", length = 255)
    private String musicTrackId;

    @Column(name = "filter", length = 100)
    private String filter;

    @Column(name = "create_mode", length = 50)
    private String createMode; // POST, STORY, REEL

    @Column(name = "cover_image_path", length = 500)
    private String coverImagePath;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}

