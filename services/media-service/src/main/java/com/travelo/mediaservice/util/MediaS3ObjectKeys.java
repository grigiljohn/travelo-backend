package com.travelo.mediaservice.util;

/**
 * Builds full S3 object keys the same way as {@code S3ObjectStorageServiceImpl} (prefix + relative path).
 */
public final class MediaS3ObjectKeys {

    private MediaS3ObjectKeys() {}

    public static String fullObjectKey(String keyPrefix, String relativePath) {
        String p = relativePath == null ? "" : relativePath.replace('\\', '/').replaceAll("^/+", "");
        String prefix = keyPrefix == null ? "" : keyPrefix.replaceAll("^/+|/+$", "");
        if (prefix.isEmpty()) {
            return p;
        }
        if (p.isEmpty()) {
            return prefix;
        }
        return prefix + "/" + p;
    }
}
