package com.travelo.discoveryservice.curated;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds the {@code app.curated-collections} section of
 * {@code application.yml} into strongly-typed seed data.
 *
 * <p>Curated collections are intentionally config-backed rather than
 * persisted to a table: editorial picks change slowly, and keeping them
 * in a YAML (or later — a Git-backed config repo) lets non-engineering
 * folks ship a new collection without a database migration. When/if we
 * need per-user collections or CMS authoring we can swap the
 * {@link CuratedCollectionsService} implementation to one that reads from
 * a table, and the public API contract stays identical.
 */
@ConfigurationProperties(prefix = "app.curated-collections")
public class CuratedCollectionsProperties {

    private List<Item> items = new ArrayList<>();

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    /**
     * A single editor-seeded collection. {@link #keywords} drives the
     * backing full-text search used to hydrate the detail view; the rest
     * are pure display fields. {@link #tag} is an optional "Editor's
     * pick" / "New" badge shown on the list card.
     */
    public static class Item {
        private String id;
        private String title;
        private String subtitle = "";
        private String coverImageUrl = "";
        private String tag;
        private List<String> keywords = new ArrayList<>();

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSubtitle() { return subtitle; }
        public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
        public String getCoverImageUrl() { return coverImageUrl; }
        public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }
        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> keywords) {
            this.keywords = keywords != null ? keywords : new ArrayList<>();
        }
    }
}
