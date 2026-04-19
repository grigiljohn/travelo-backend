package com.travelo.mediaservice.service;

import com.travelo.mediaservice.entity.MediaFile;
import com.travelo.mediaservice.entity.MediaType;
import com.travelo.mediaservice.entity.MediaVariant;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Picks which blob to serve for {@code GET /v1/media/files/{id}} and for download URLs without an explicit variant.
 * Prefer processed delivery assets (video {@code playback}, image {@code preview}) when present.
 */
@Component
public class MediaContentResolutionService {

    public record ResolvedContent(String storageKey, String contentType, String downloadFilename) {}

    public ResolvedContent resolve(MediaFile media, String variantQueryParam) {
        if (media == null) {
            throw new IllegalArgumentException("media is required");
        }
        String v = variantQueryParam == null ? "" : variantQueryParam.trim();
        if ("original".equalsIgnoreCase(v)) {
            return new ResolvedContent(
                    media.getStorageKey(),
                    media.getMimeType() != null ? media.getMimeType() : "application/octet-stream",
                    dispositionFilename(media, media.getMimeType())
            );
        }
        if (!v.isEmpty()) {
            Optional<MediaVariant> found = findVariant(media.getVariants(), v);
            if (found.isEmpty()) {
                return new ResolvedContent(null, null, null);
            }
            MediaVariant mv = found.get();
            String mime = mv.getMime() != null && !mv.getMime().isBlank()
                    ? mv.getMime()
                    : guessMimeFromVariantName(mv.getName());
            return new ResolvedContent(mv.getKey(), mime, dispositionFilename(media, mime));
        }

        // Default: smart delivery
        MediaType mt = media.getMediaType();
        if (mt == MediaType.VIDEO) {
            Optional<MediaVariant> playback = findVariant(media.getVariants(), "playback");
            if (playback.isPresent()) {
                return new ResolvedContent(
                        playback.get().getKey(),
                        "video/mp4",
                        dispositionFilename(media, "video/mp4")
                );
            }
        } else if (mt == MediaType.IMAGE) {
            String origMime = media.getMimeType() == null ? "" : media.getMimeType().toLowerCase(Locale.ROOT);
            boolean isGif = origMime.contains("gif");
            if (!isGif) {
                Optional<MediaVariant> preview = findVariant(media.getVariants(), "preview");
                if (preview.isPresent()) {
                    String mime = preview.get().getMime() != null ? preview.get().getMime() : "image/jpeg";
                    return new ResolvedContent(
                            preview.get().getKey(),
                            mime,
                            dispositionFilename(media, mime)
                    );
                }
            }
        }

        return new ResolvedContent(
                media.getStorageKey(),
                media.getMimeType() != null ? media.getMimeType() : "application/octet-stream",
                dispositionFilename(media, media.getMimeType())
        );
    }

    private static Optional<MediaVariant> findVariant(List<MediaVariant> variants, String name) {
        if (variants == null || name == null || name.isBlank()) {
            return Optional.empty();
        }
        String want = name.trim().toLowerCase(Locale.ROOT);
        return variants.stream()
                .filter(x -> x != null && x.getName() != null && want.equals(x.getName().trim().toLowerCase(Locale.ROOT)))
                .findFirst();
    }

    private static String guessMimeFromVariantName(String name) {
        if (name == null) {
            return "application/octet-stream";
        }
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "playback" -> "video/mp4";
            case "preview", "thumb", "thumbnail" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }

    private static String baseName(String filename) {
        if (filename == null || filename.isBlank()) {
            return "media";
        }
        int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        String n = slash >= 0 ? filename.substring(slash + 1) : filename;
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }

    private static String dispositionFilename(MediaFile media, String mimeOrNull) {
        String base = baseName(media.getFilename());
        String m = mimeOrNull == null ? "" : mimeOrNull.toLowerCase(Locale.ROOT);
        if (m.startsWith("video/")) {
            return base + ".mp4";
        }
        if (m.startsWith("image/")) {
            if (m.contains("png")) {
                return base + ".png";
            }
            if (m.contains("webp")) {
                return base + ".webp";
            }
            return base + ".jpg";
        }
        return media.getFilename() != null ? media.getFilename() : "media";
    }
}
