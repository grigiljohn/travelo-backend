package com.travelo.admin.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "predefined_trips")
@Getter
@Setter
@NoArgsConstructor
public class PredefinedTrip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120, unique = true)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String subtitle = "";

    @Column(name = "hero_image_url", nullable = false, columnDefinition = "text")
    private String heroImageUrl = "";

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "estimated_days")
    private Integer estimatedDays;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trip_preferences", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> tripPreferences = new HashMap<>();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
