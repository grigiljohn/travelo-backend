package com.travelo.admin.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "feature_flags")
@Getter
@Setter
@NoArgsConstructor
public class FeatureFlag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "feature_name", nullable = false, length = 100, unique = true)
    private String featureName;
    @Column(name = "is_enabled", nullable = false)
    private boolean enabled;
    @Column(name = "rollout_percentage", nullable = false)
    private int rolloutPercentage;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FeaturePlatform platform = FeaturePlatform.WEB;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
