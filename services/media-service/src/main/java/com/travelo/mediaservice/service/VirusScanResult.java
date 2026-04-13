package com.travelo.mediaservice.service;

/**
 * Result of virus scanning operation.
 */
public record VirusScanResult(boolean clean, String threatName, String details) {
    public static VirusScanResult createClean() {
        return new VirusScanResult(true, null, null);
    }

    public static VirusScanResult createInfected(String threatName, String details) {
        return new VirusScanResult(false, threatName, details);
    }
}
