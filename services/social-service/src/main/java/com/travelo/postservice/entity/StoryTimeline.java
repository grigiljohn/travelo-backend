package com.travelo.postservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "story_timelines", indexes = {
    @Index(name = "idx_story_timelines_user_id", columnList = "user_id"),
    @Index(name = "idx_story_timelines_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryTimeline {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "media_order", columnDefinition = "jsonb", nullable = false)
    private List<String> mediaOrder; // Array of media IDs in order

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "durations", columnDefinition = "jsonb", nullable = false)
    private Map<String, Integer> durations; // Map of media_id -> duration_seconds

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transitions", columnDefinition = "jsonb")
    private List<Map<String, Object>> transitions; // Array of transition configs

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "text_overlays", columnDefinition = "jsonb")
    private List<Map<String, Object>> textOverlays; // Array of text overlay configs

    @Column(name = "template_id", length = 50)
    private String templateId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (durations == null) {
            durations = new HashMap<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

