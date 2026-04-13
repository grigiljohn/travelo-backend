package com.travelo.notificationservice.repository;

import com.travelo.notificationservice.entity.NotificationPreference;
import com.travelo.notificationservice.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
    
    Optional<NotificationPreference> findByUserIdAndNotificationType(UUID userId, NotificationType notificationType);
}

