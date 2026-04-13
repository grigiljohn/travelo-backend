package com.travelo.authservice.repository;

import com.travelo.authservice.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    
    Optional<Device> findByUserIdAndDeviceId(UUID userId, String deviceId);
    
    List<Device> findByUserIdOrderByLastUsedAtDesc(UUID userId);
    
    boolean existsByUserIdAndDeviceId(UUID userId, String deviceId);
    
    void deleteByUserIdAndDeviceId(UUID userId, String deviceId);
}

