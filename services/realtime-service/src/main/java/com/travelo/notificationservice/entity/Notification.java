package com.travelo.notificationservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_user_id", columnList = "user_id"),
    @Index(name = "idx_notifications_read", columnList = "read"),
    @Index(name = "idx_notifications_created_at", columnList = "created_at"),
    @Index(name = "idx_notifications_user_created", columnList = "user_id, created_at")
})
public class Notification {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    private UUID userId; // recipient

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private Map<String, Object> data; // Additional metadata

    @Column(name = "actor_id", columnDefinition = "UUID")
    private UUID actorId; // User who triggered the notification

    @Column(name = "target_id", columnDefinition = "UUID")
    private UUID targetId; // Target entity (post_id, comment_id, etc.)

    @Column(name = "target_type", length = 50)
    private String targetType; // 'POST', 'COMMENT', 'USER', 'MESSAGE'

    @Column(nullable = false)
    private Boolean read = false;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(nullable = false)
    private Boolean pushed = false;

    @Column(name = "pushed_at")
    private OffsetDateTime pushedAt;

    @Column(name = "in_app_delivered", nullable = false)
    private Boolean inAppDelivered = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public NotificationType getNotificationType() { return notificationType; }
    public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }
    public OffsetDateTime getReadAt() { return readAt; }
    public void setReadAt(OffsetDateTime readAt) { this.readAt = readAt; }
    public Boolean getPushed() { return pushed; }
    public void setPushed(Boolean pushed) { this.pushed = pushed; }
    public OffsetDateTime getPushedAt() { return pushedAt; }
    public void setPushedAt(OffsetDateTime pushedAt) { this.pushedAt = pushedAt; }
    public Boolean getInAppDelivered() { return inAppDelivered; }
    public void setInAppDelivered(Boolean inAppDelivered) { this.inAppDelivered = inAppDelivered; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", userId=" + userId +
                ", notificationType=" + notificationType +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
                ", data=" + data +
                ", actorId=" + actorId +
                ", targetId=" + targetId +
                ", targetType='" + targetType + '\'' +
                ", read=" + read +
                ", readAt=" + readAt +
                ", pushed=" + pushed +
                ", pushedAt=" + pushedAt +
                ", inAppDelivered=" + inAppDelivered +
                ", createdAt=" + createdAt +
                '}';
    }
}

