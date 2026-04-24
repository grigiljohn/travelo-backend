package com.travelo.postservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Lightweight summary of a user's saved-posts collection.
 *
 * <p>Returned by {@code GET /api/v1/posts/saved/collections} to power the
 * collections strip on the mobile "Saved" screen. Cover URL is the first media
 * item of the most recently saved post in the collection, if any.
 */
public class SavedCollectionDto {

    @JsonProperty("name")
    private String name;

    @JsonProperty("count")
    private long count;

    @JsonProperty("cover_url")
    private String coverUrl;

    public SavedCollectionDto() {}

    public SavedCollectionDto(String name, long count, String coverUrl) {
        this.name = name;
        this.count = count;
        this.coverUrl = coverUrl;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
}
