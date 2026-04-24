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
    @Column(nullable = false, length = 200)
    private String city = "";
    @Column(nullable = false, length = 16)
    private String visibility = "public";
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
