package com.travelo.circlesservice.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "circle_community_members")
@IdClass(MemberPk.class)
@Getter
@Setter
@NoArgsConstructor
public class CircleCommunityMemberEntity {

    @Id
    @Column(name = "community_id", length = 64)
    private String communityId;

    @Id
    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(nullable = false, length = 16)
    private String role = "MEMBER";

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", nullable = false, length = 16)
    private MembershipStatus membershipStatus = MembershipStatus.ACTIVE;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    @PrePersist
    void prePersist() {
        if (joinedAt == null) {
            joinedAt = OffsetDateTime.now();
        }
    }
}
