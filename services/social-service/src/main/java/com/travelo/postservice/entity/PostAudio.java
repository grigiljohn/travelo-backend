package com.travelo.postservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_audio", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"post_id", "audio_id"})
}, indexes = {
    @Index(name = "idx_post_audio_post_id", columnList = "post_id"),
    @Index(name = "idx_post_audio_audio_id", columnList = "audio_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostAudio {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "post_id", nullable = false, length = 50)
    private String postId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_id", nullable = false)
    private AudioLibrary audio;

    @Column(name = "fade_in_duration")
    @Builder.Default
    private Integer fadeInDuration = 0; // milliseconds

    @Column(name = "fade_out_duration")
    @Builder.Default
    private Integer fadeOutDuration = 0; // milliseconds

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal volume = BigDecimal.ONE; // 0.0 to 1.0

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

