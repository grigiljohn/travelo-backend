package com.travelo.postservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_locations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"post_id", "location_id"})
}, indexes = {
    @Index(name = "idx_post_locations_post_id", columnList = "post_id"),
    @Index(name = "idx_post_locations_location_id", columnList = "location_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostLocation {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "post_id", nullable = false, length = 50)
    private String postId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

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

