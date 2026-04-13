package com.travelo.adservice.entity;

import com.travelo.adservice.entity.enums.ChangeAction;
import com.travelo.adservice.entity.enums.ChangeType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "change_history", indexes = {
    @Index(name = "idx_type", columnList = "type"),
    @Index(name = "idx_action", columnList = "action"),
    @Index(name = "idx_entity_id", columnList = "entity_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class ChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChangeType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChangeAction action;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "business_account_id")
    private UUID businessAccountId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> changes = new HashMap<>();

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ChangeType getType() {
        return type;
    }

    public void setType(ChangeType type) {
        this.type = type;
    }

    public ChangeAction getAction() {
        return action;
    }

    public void setAction(ChangeAction action) {
        this.action = action;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getBusinessAccountId() {
        return businessAccountId;
    }

    public void setBusinessAccountId(UUID businessAccountId) {
        this.businessAccountId = businessAccountId;
    }

    public Map<String, Object> getChanges() {
        return changes;
    }

    public void setChanges(Map<String, Object> changes) {
        this.changes = changes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

