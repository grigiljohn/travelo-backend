package com.travelo.admin.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "admin_communities")
@Getter
@Setter
@NoArgsConstructor
public class AdminManagedCommunity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "external_id", length = 64)
    private String externalId;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description = "";
    @Column(nullable = false, length = 500)
    private String tagline = "";
    @Column(nullable = false, length = 200)
    private String city = "";
    @Column(name = "cover_image_url", nullable = false, length = 2048)
    private String coverImageUrl = "";
    @Column(name = "icon_image_url", nullable = false, length = 2048)
    private String iconImageUrl = "";
    @Column(name = "tags_csv", nullable = false, columnDefinition = "TEXT")
    private String tagsCsv = "";
    @Column(name = "topics_csv", nullable = false, columnDefinition = "TEXT")
    private String topicsCsv = "";
    @Column(name = "rules_text", nullable = false, columnDefinition = "TEXT")
    private String rulesText = "";
    @Column(name = "member_count", nullable = false)
    private int memberCount = 0;
    @Column(name = "online_count", nullable = false)
    private int onlineCount = 0;
    @Column(name = "require_admin_approval", nullable = false)
    private boolean requireAdminApproval = false;
    @Column(name = "allow_member_invites", nullable = false)
    private boolean allowMemberInvites = true;
    @Column(nullable = false, length = 16)
    private String visibility = "public";
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
