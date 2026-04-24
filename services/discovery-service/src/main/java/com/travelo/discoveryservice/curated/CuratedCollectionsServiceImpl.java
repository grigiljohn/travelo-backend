package com.travelo.discoveryservice.curated;

import com.travelo.searchservice.dto.SearchResponse;
import com.travelo.searchservice.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Default {@link CuratedCollectionsService}. Seed data comes from
 * {@link CuratedCollectionsProperties} (YAML) and note hydration goes
 * through {@link SearchService#searchPosts} so we inherit the same
 * Elasticsearch ranking, privacy filtering, and pagination behaviour
 * the rest of the search endpoints use.
 *
 * <p>The count query for list rows is intentionally small (limit = 1)
 * so we pay for the ranking once and pull the {@code total_results}
 * field out rather than loading a full page of hits.
 */
@Service
@EnableConfigurationProperties(CuratedCollectionsProperties.class)
public class CuratedCollectionsServiceImpl implements CuratedCollectionsService {

    private static final Logger logger = LoggerFactory.getLogger(CuratedCollectionsServiceImpl.class);

    private final CuratedCollectionsProperties properties;
    private final SearchService searchService;

    public CuratedCollectionsServiceImpl(
            CuratedCollectionsProperties properties,
            SearchService searchService) {
        this.properties = properties;
        this.searchService = searchService;
    }

    @Override
    public List<CuratedCollectionDto> listCollections(String viewerId) {
        final List<CuratedCollectionsProperties.Item> items = properties.getItems();
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        final List<CuratedCollectionDto> out = new ArrayList<>(items.size());
        for (CuratedCollectionsProperties.Item item : items) {
            if (item.getId() == null || item.getId().isBlank()) continue;
            int noteCount = 0;
            try {
                final String q = joinKeywords(item.getKeywords());
                // limit=1 is a deliberate micro-opt: ES still returns the
                // total count even when we only ask for one result.
                final SearchResponse resp = searchService.searchPosts(q, 1, 1, viewerId);
                if (resp != null && resp.getTotalResults() != null) {
                    noteCount = Math.toIntExact(Math.min(resp.getTotalResults(), Integer.MAX_VALUE));
                }
            } catch (RuntimeException e) {
                logger.debug("Curated collection '{}' count lookup failed: {}",
                        item.getId(), e.getMessage());
            }
            out.add(new CuratedCollectionDto(
                    item.getId(),
                    item.getTitle(),
                    item.getSubtitle(),
                    item.getCoverImageUrl(),
                    noteCount,
                    item.getTag()
            ));
        }
        return out;
    }

    @Override
    public Optional<CuratedCollectionDetailDto> getCollectionDetail(
            String id,
            int page,
            int limit,
            String viewerId) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        final Optional<CuratedCollectionsProperties.Item> match = properties.getItems().stream()
                .filter(it -> id.equalsIgnoreCase(it.getId()))
                .findFirst();
        if (match.isEmpty()) {
            return Optional.empty();
        }
        final CuratedCollectionsProperties.Item item = match.get();
        final int safePage = Math.max(1, page);
        final int safeLimit = Math.min(Math.max(1, limit), 50);
        final String q = joinKeywords(item.getKeywords());

        final SearchResponse resp;
        try {
            resp = searchService.searchPosts(q, safePage, safeLimit, viewerId);
        } catch (RuntimeException e) {
            logger.warn("Failed to hydrate curated collection '{}' notes: {}", id, e.getMessage());
            return Optional.of(new CuratedCollectionDetailDto(
                    item.getId(),
                    item.getTitle(),
                    item.getSubtitle(),
                    item.getCoverImageUrl(),
                    item.getTag(),
                    0,
                    safePage,
                    safeLimit,
                    Boolean.FALSE,
                    Collections.emptyList()
            ));
        }

        final int total = resp != null && resp.getTotalResults() != null
                ? Math.toIntExact(Math.min(resp.getTotalResults(), Integer.MAX_VALUE))
                : 0;
        return Optional.of(new CuratedCollectionDetailDto(
                item.getId(),
                item.getTitle(),
                item.getSubtitle(),
                item.getCoverImageUrl(),
                item.getTag(),
                total,
                safePage,
                safeLimit,
                resp != null ? resp.getHasMore() : Boolean.FALSE,
                resp != null ? resp.getResults() : Collections.emptyList()
        ));
    }

    /**
     * Build the backing search query. When no keywords are configured
     * we fall back to the wildcard so the collection still shows popular
     * posts rather than 404-ing — editors sometimes forget to add the
     * keyword list, and "some content" beats "empty page" every time.
     */
    private String joinKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return "*";
        return String.join(" ", keywords).trim();
    }
}
