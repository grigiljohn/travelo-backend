package com.travelo.circlesservice.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "circle_communities")
@Getter
@Setter
@NoArgsConstructor
public class CircleCommunityEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 4000)
    private String description = "";

    @Column(nullable = false, length = 500)
    private String tagline = "";

    @Column(name = "cover_image_url", length = 2048)
    private String coverImageUrl;

    @Column(name = "icon_image_url", length = 2048)
    private String iconImageUrl;

    @Column(name = "rules_text", nullable = false, columnDefinition = "TEXT")
    private String rulesText = "";

    @Convert(converter = ListStringJsonConverter.class)
    @Column(name = "topics_json", nullable = false, columnDefinition = "TEXT")
    private List<String> topics = new ArrayList<>();

    @Column(name = "require_admin_approval", nullable = false)
    private boolean requireAdminApproval = false;

    @Column(name = "allow_member_invites", nullable = false)
    private boolean allowMemberInvites = true;

    @Column(nullable = false, length = 200)
    private String city = "";

    @Column(nullable = false, length = 16)
    private String visibility = "public";

    @Column(name = "owner_user_id", nullable = false, length = 64)
    private String ownerUserId;

    @Column(name = "member_count", nullable = false)
    private int memberCount = 1;

    @Column(name = "last_activity_label", nullable = false, length = 64)
    private String lastActivityLabel = "Just now";

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
