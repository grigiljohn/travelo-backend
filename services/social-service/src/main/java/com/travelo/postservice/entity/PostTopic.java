package com.travelo.postservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_topics", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"post_id", "topic_id"})
}, indexes = {
    @Index(name = "idx_post_topics_post_id", columnList = "post_id"),
    @Index(name = "idx_post_topics_topic_id", columnList = "topic_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostTopic {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "post_id", nullable = false, length = 50)
    private String postId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = OffsetDateTime.now();
    }
}

