package com.travelo.admin.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "moderation_actions")
@Getter
@Setter
@NoArgsConstructor
public class ModerationAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "report_id")
    private Long reportId;
    @Column(name = "actor_id", nullable = false, length = 64)
    private String actorId;
    @Column(name = "action_type", nullable = false, length = 32)
    private String actionType;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String note = "";
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
