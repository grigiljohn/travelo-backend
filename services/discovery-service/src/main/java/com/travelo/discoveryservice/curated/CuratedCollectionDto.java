package com.travelo.discoveryservice.curated;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * List-row DTO for a curated collection. Deliberately small so the
 * collections carousel / grid can render quickly without needing to
 * hydrate any note content; the detail endpoint returns items.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CuratedCollectionDto(
        String id,
        String title,
        String subtitle,
        String coverImageUrl,
        Integer noteCount,
        String tag
) {
}
