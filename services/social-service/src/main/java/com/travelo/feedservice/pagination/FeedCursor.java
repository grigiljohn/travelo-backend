package com.travelo.feedservice.pagination;

/**
 * Opaque cursor payload carried between paginated feed requests.
 *
 * <p>Every field is stable across a single user session: it pins the feed
 * context (surface + mood) so that mid-scroll filter flips don't silently
 * return stale pages, and it carries both a {@code lastPostId} anchor and a
 * {@code position} fallback so deep pagination still works when seen-state
 * suppression drops the anchor post between requests.</p>
 *
 * <p>Cursors are serialized as HMAC-signed Base64 JSON blobs by
 * {@link FeedCursorCodec}. Clients should treat them as fully opaque.</p>
 */
public final class FeedCursor {

    public static final int CURRENT_VERSION = 1;

    private final int version;
    private final String lastPostId;
    private final int position;
    private final String surface;
    private final String mood;
    private final long issuedAtEpochMillis;

    public FeedCursor(
            int version,
            String lastPostId,
            int position,
            String surface,
            String mood,
            long issuedAtEpochMillis) {
        this.version = version;
        this.lastPostId = lastPostId;
        this.position = position;
        this.surface = surface;
        this.mood = mood;
        this.issuedAtEpochMillis = issuedAtEpochMillis;
    }

    public static FeedCursor of(
            String lastPostId,
            int position,
            String surface,
            String mood,
            long issuedAtEpochMillis) {
        return new FeedCursor(
                CURRENT_VERSION,
                lastPostId,
                Math.max(0, position),
                surface,
                mood,
                issuedAtEpochMillis
        );
    }

    public int getVersion() {
        return version;
    }

    public String getLastPostId() {
        return lastPostId;
    }

    public int getPosition() {
        return position;
    }

    public String getSurface() {
        return surface;
    }

    public String getMood() {
        return mood;
    }

    public long getIssuedAtEpochMillis() {
        return issuedAtEpochMillis;
    }

    /**
     * True when {@code other} pins the same filtering context (surface + mood)
     * this cursor was issued for. When context drifts we should start fresh
     * from the top of the ranked list instead of pretending the cursor still
     * points to a meaningful offset.
     */
    public boolean matchesContext(String requestSurface, String requestMood) {
        return safeEquals(surface, requestSurface) && safeEquals(mood, requestMood);
    }

    private static boolean safeEquals(String a, String b) {
        if (a == null || a.isBlank()) {
            return b == null || b.isBlank();
        }
        return a.equalsIgnoreCase(b);
    }
}
