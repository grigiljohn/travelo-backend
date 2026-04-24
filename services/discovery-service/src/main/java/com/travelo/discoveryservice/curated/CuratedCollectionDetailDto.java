package com.travelo.discoveryservice.curated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.travelo.searchservice.dto.SearchResultItem;

import java.util.List;

/**
 * Detail-view DTO — the collection row plus the backing notes page
 * expanded inline. {@code notes} items reuse {@link SearchResultItem} so
 * the mobile side can render them with the same card it already uses
 * for explore/search hits.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CuratedCollectionDetailDto(
        String id,
        String title,
        String subtitle,
        String coverImageUrl,
        String tag,
        Integer noteCount,
        Integer page,
        Integer limit,
        Boolean hasMore,
        List<SearchResultItem> notes
) {
}
