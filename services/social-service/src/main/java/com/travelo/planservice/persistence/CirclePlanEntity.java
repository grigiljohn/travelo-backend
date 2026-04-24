package com.travelo.planservice.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Persisted circle event / travel plan (see Flyway {@code V22__create_circle_plans_tables.sql}).
 */
@Entity
@Table(
        name = "circle_plans",
        indexes = {
                @Index(name = "idx_circle_plans_host_user", columnList = "host_user_id"),
                @Index(name = "idx_circle_plans_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CirclePlanEntity {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "host_user_id", nullable = false, length = 64)
    private String hostUserId;

    @Column(name = "organizer_community_id", length = 64)
    private String organizerCommunityId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description = "";

    @Column(name = "location_label", nullable = false, length = 300)
    private String locationLabel;

    @Column(name = "external_place_id", length = 128)
    private String externalPlaceId;

    private Double latitude;

    private Double longitude;

    @Column(name = "starts_at")
    private OffsetDateTime startsAt;

    @Column(name = "time_label", nullable = false, length = 200)
    private String timeLabel;

    @Column(name = "max_people", nullable = false)
    private int maxPeople;

    @Column(name = "joined_count", nullable = false)
    private int joinedCount = 1;

    @Column(nullable = false, length = 24)
    private String badge = "NONE";

    @Column(name = "hero_image_url", nullable = false, length = 2000)
    private String heroImageUrl = "";

    @Column(name = "host_name", nullable = false, length = 120)
    private String hostName;

    @Column(name = "host_avatar_url", nullable = false, length = 2000)
    private String hostAvatarUrl = "";

    @Column(nullable = false, length = 32)
    private String privacy = "PUBLIC";

    @Column(name = "require_approval", nullable = false)
    private boolean requireApproval;

    @Column(name = "allow_waitlist", nullable = false)
    private boolean allowWaitlist = true;

    @Column(nullable = false, length = 24)
    private String status = "PUBLISHED";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        var now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
