package com.travelo.mediaservice.service;

/**
 * Service for virus scanning uploaded media files.
 * Can be implemented with ClamAV, commercial APIs (OPSWAT), or AWS Lambda.
 */
public interface VirusScanService {

    /**
     * Scan a file from S3 for viruses.
     * @param bucket S3 bucket name
     * @param key S3 object key
     * @return true if clean, false if infected
     */
    boolean scanFile(String bucket, String key);

    /**
     * Get scan result details (optional).
     * @param bucket S3 bucket name
     * @param key S3 object key
     * @return Scan result details or null if not scanned
     */
    VirusScanResult getScanResult(String bucket, String key);
}
