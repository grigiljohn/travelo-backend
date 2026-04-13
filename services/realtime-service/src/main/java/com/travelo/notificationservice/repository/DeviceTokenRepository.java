package com.travelo.notificationservice.repository;

import com.travelo.notificationservice.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    
    List<DeviceToken> findByUserId(UUID userId);
    
    Optional<DeviceToken> findByUserIdAndDeviceIdAndToken(UUID userId, String deviceId, String token);
    
    void deleteByToken(String token);
}

