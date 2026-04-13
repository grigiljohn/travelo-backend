package com.travelo.mediaservice.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Utility for generating local storage paths.
 * Layout: uploads/raw/YYYY/MM/DD/{mediaId}/original.ext
 * Processed: uploads/processed/{mediaId}/variants/{variant-name}.ext
 */
public class LocalStorageKeyGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * @param mediaId media UUID
     * @param extension file extension (e.g. ".jpg") or null for "original"
     */
    public static String rawOriginalKey(UUID mediaId, String extension) {
        LocalDate now = LocalDate.now();
        String filename = (extension != null && !extension.isEmpty())
                ? "original" + (extension.startsWith(".") ? extension : "." + extension)
                : "original";
        return String.format("raw/%s/%s/%s",
                now.format(DATE_FORMATTER),
                mediaId,
                filename);
    }

    /** @deprecated Use {@link #rawOriginalKey(UUID, String)} with extension */
    public static String rawOriginalKey(UUID mediaId) {
        return rawOriginalKey(mediaId, null);
    }

    public static String processedVariantKey(UUID mediaId, String variantName) {
        return String.format("processed/%s/variants/%s", mediaId, variantName);
    }
}
