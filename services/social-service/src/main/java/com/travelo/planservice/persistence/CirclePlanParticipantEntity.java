package com.travelo.planservice.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "circle_plan_participants",
        uniqueConstraints = @UniqueConstraint(name = "uq_circle_plan_participant", columnNames = {"plan_id", "user_id"}),
        indexes = {
                @Index(name = "idx_circle_plan_participants_plan", columnList = "plan_id"),
                @Index(name = "idx_circle_plan_participants_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CirclePlanParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID planId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "display_name", length = 120)
    private String displayName;

    @Column(name = "avatar_url", nullable = false, length = 2000)
    private String avatarUrl = "";

    @Column(nullable = false, length = 16)
    private String role = "MEMBER";

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    @PrePersist
    void prePersist() {
        if (joinedAt == null) {
            joinedAt = OffsetDateTime.now();
        }
    }
}
