package com.travelo.authservice.service;

import com.travelo.authservice.entity.Device;
import com.travelo.authservice.repository.DeviceRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);
    
    private final DeviceRepository deviceRepository;
    
    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }
    
    /**
     * Generate a unique device fingerprint from request headers
     */
    public String generateDeviceId(HttpServletRequest request) {
        try {
            String userAgent = request.getHeader("User-Agent");
            String acceptLanguage = request.getHeader("Accept-Language");
            String acceptEncoding = request.getHeader("Accept-Encoding");
            
            // Create a fingerprint from browser characteristics
            String fingerprint = String.format("%s|%s|%s", 
                userAgent != null ? userAgent : "",
                acceptLanguage != null ? acceptLanguage : "",
                acceptEncoding != null ? acceptEncoding : ""
            );
            
            // Hash the fingerprint to create a unique device ID
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(fingerprint.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("Error generating device ID", e);
            // Fallback to a simple hash
            return String.valueOf(request.getHeader("User-Agent").hashCode());
        }
    }
    
    /**
     * Extract device name from user agent
     */
    public String extractDeviceName(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown Device";
        }
        
        // Simple device name extraction
        if (userAgent.contains("iPhone")) {
            return "iPhone";
        } else if (userAgent.contains("iPad")) {
            return "iPad";
        } else if (userAgent.contains("Android")) {
            return "Android Device";
        } else if (userAgent.contains("Windows")) {
            if (userAgent.contains("Chrome")) {
                return "Chrome on Windows";
            } else if (userAgent.contains("Firefox")) {
                return "Firefox on Windows";
            } else if (userAgent.contains("Edge")) {
                return "Edge on Windows";
            }
            return "Windows Device";
        } else if (userAgent.contains("Mac")) {
            if (userAgent.contains("Chrome")) {
                return "Chrome on Mac";
            } else if (userAgent.contains("Safari")) {
                return "Safari on Mac";
            }
            return "Mac Device";
        } else if (userAgent.contains("Linux")) {
            return "Linux Device";
        }
        
        return "Unknown Device";
    }
    
    /**
     * Determine device type from user agent
     */
    public String extractDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "UNKNOWN";
        }
        
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("iphone") || ua.contains("android")) {
            return "MOBILE";
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            return "TABLET";
        } else {
            return "DESKTOP";
        }
    }
    
    /**
     * Get client IP address from request
     */
    public String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Handle multiple IPs (take the first one)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    /**
     * Register or update device for user
     */
    @Transactional
    public Device registerOrUpdateDevice(UUID userId, HttpServletRequest request) {
        String deviceId = generateDeviceId(request);
        String userAgent = request.getHeader("User-Agent");
        String deviceName = extractDeviceName(userAgent);
        String deviceType = extractDeviceType(userAgent);
        String ipAddress = getClientIpAddress(request);
        
        Optional<Device> existingDevice = deviceRepository.findByUserIdAndDeviceId(userId, deviceId);
        
        Device device;
        if (existingDevice.isPresent()) {
            device = existingDevice.get();
            device.setLastUsedAt(java.time.OffsetDateTime.now());
            device.setIpAddress(ipAddress);
            logger.info("Updated existing device: {} for user: {}", deviceId, userId);
        } else {
            device = new Device(userId, deviceId, deviceName, deviceType, userAgent, ipAddress);
            logger.info("Registered new device: {} ({}) for user: {}", deviceId, deviceName, userId);
        }
        
        return deviceRepository.save(device);
    }
    
    /**
     * Check if device is trusted
     */
    public boolean isDeviceTrusted(UUID userId, String deviceId) {
        Optional<Device> device = deviceRepository.findByUserIdAndDeviceId(userId, deviceId);
        return device.map(Device::getIsTrusted).orElse(false);
    }
    
    /**
     * Mark device as trusted
     */
    @Transactional
    public Device markDeviceAsTrusted(UUID userId, String deviceId) {
        Device device = deviceRepository.findByUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        device.setIsTrusted(true);
        return deviceRepository.save(device);
    }
    
    /**
     * Get all devices for a user
     */
    public List<Device> getUserDevices(UUID userId) {
        return deviceRepository.findByUserIdOrderByLastUsedAtDesc(userId);
    }
    
    /**
     * Remove device
     */
    @Transactional
    public void removeDevice(UUID userId, String deviceId) {
        deviceRepository.deleteByUserIdAndDeviceId(userId, deviceId);
        logger.info("Removed device: {} for user: {}", deviceId, userId);
    }
}

