package com.travelo.mediaservice.service.impl;

import com.travelo.mediaservice.service.LocalStorageService;
import com.travelo.mediaservice.service.VirusScanResult;
import com.travelo.mediaservice.service.VirusScanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Virus scanning for local storage.
 * Stub implementation - returns clean for all files.
 * TODO: Integrate ClamAV or similar for actual scanning.
 */
@Service
@ConditionalOnProperty(name = "media.virus-scan.enabled", havingValue = "true", matchIfMissing = true)
public class VirusScanServiceImpl implements VirusScanService {

    private static final Logger log = LoggerFactory.getLogger(VirusScanServiceImpl.class);
    private final LocalStorageService localStorageService;

    public VirusScanServiceImpl(LocalStorageService localStorageService) {
        this.localStorageService = localStorageService;
    }

    @Override
    public boolean scanFile(String bucket, String key) {
        if (!"local".equals(bucket)) {
            log.warn("Virus scan only supports local storage, bucket={}", bucket);
            return true;
        }
        if (!localStorageService.exists(key)) {
            log.warn("File not found for scan: {}", key);
            return false;
        }
        try {
            // TODO: Implement actual virus scanning (ClamAV, etc.)
            log.debug("Virus scan completed for key={} - clean (stub)", key);
            return true;
        } catch (Exception e) {
            log.error("Error scanning file: key={}", key, e);
            return false;
        }
    }

    @Override
    public VirusScanResult getScanResult(String bucket, String key) {
        boolean clean = scanFile(bucket, key);
        return clean ? VirusScanResult.createClean() : VirusScanResult.createInfected("unknown", "Scan failed");
    }
}
