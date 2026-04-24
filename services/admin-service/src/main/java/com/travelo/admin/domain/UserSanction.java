package com.travelo.admin.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_sanctions")
@Getter
@Setter
@NoArgsConstructor
public class UserSanction {
    @Id
    @Column(name = "user_id", length = 64)
    private String userId;
    private boolean banned;
    private boolean restricted;
    @Column(name = "banned_at")
    private OffsetDateTime bannedAt;
    @Column(name = "restricted_at")
    private OffsetDateTime restrictedAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
