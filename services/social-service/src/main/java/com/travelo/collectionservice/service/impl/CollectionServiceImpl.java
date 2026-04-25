package com.travelo.collectionservice.service.impl;

import com.travelo.collectionservice.dto.*;
import com.travelo.collectionservice.entity.Collection;
import com.travelo.collectionservice.entity.CollectionMedia;
import com.travelo.collectionservice.entity.CollectionMediaSourceType;
import com.travelo.collectionservice.entity.CollectionMediaType;
import com.travelo.collectionservice.entity.CollectionType;
import com.travelo.collectionservice.repository.CollectionMediaRepository;
import com.travelo.collectionservice.repository.CollectionRepository;
import com.travelo.collectionservice.service.CollectionService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CollectionServiceImpl implements CollectionService {

    private final CollectionRepository collectionRepository;
    private final CollectionMediaRepository collectionMediaRepository;

    public CollectionServiceImpl(CollectionRepository collectionRepository, CollectionMediaRepository collectionMediaRepository) {
        this.collectionRepository = collectionRepository;
        this.collectionMediaRepository = collectionMediaRepository;
    }

    @Override
    @Transactional
    public CollectionSummaryDto create(String userId, CreateCollectionRequest request) {
        Collection c = new Collection();
        c.setUserId(userId);
        c.setTitle(request.title().trim());
        c.setType(request.type());
        c.setTripId(trimToNull(request.tripId()));
        c.setCoverImageUrl(trimToNull(request.coverImageUrl()));
        Collection saved = collectionRepository.save(c);
        return toSummary(saved, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CollectionSummaryDto> list(String userId, CollectionType type, Pageable pageable) {
        Page<Collection> page = type == null
                ? collectionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                : collectionRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);
        Map<UUID, Long> counts = countMediaFor(page.getContent());
        return page.map(c -> toSummary(c, counts.getOrDefault(c.getId(), 0L)));
    }

    @Override
    @Transactional(readOnly = true)
    public CollectionDetailDto getDetail(String userId, UUID collectionId) {
        Collection c = ownedCollection(userId, collectionId);
        long count = collectionMediaRepository.countByCollectionIds(List.of(c.getId())).stream()
                .findFirst()
                .map(r -> ((Number) r[1]).longValue())
                .orElse(0L);
        List<CollectionMediaDto> preview = collectionMediaRepository.findTop20ByCollectionOrderByCreatedAtDesc(c).stream()
                .map(this::toMediaDto)
                .toList();
        return new CollectionDetailDto(
                c.getId(),
                c.getTitle(),
                c.getType(),
                c.getTripId(),
                c.getCoverImageUrl(),
                count,
                c.getCreatedAt(),
                preview
        );
    }

    @Override
    @Transactional
    public void addMedia(String userId, UUID collectionId, AddCollectionMediaRequest request) {
        Collection c = ownedCollection(userId, collectionId);
        int nextBase = collectionMediaRepository.findTop20ByCollectionOrderByCreatedAtDesc(c).size();
        int idx = 0;
        for (AddCollectionMediaItemRequest item : request.items()) {
            CollectionMedia m = new CollectionMedia();
            m.setCollection(c);
            m.setMediaUrl(item.mediaUrl());
            m.setThumbnailUrl(trimToNull(item.thumbnailUrl()));
            m.setMediaType(item.mediaType() == null ? CollectionMediaType.IMAGE : item.mediaType());
            m.setSourceType(item.sourceType() == null ? CollectionMediaSourceType.DEVICE : item.sourceType());
            m.setSourceId(trimToNull(item.sourceId()));
            m.setCapturedAt(item.capturedAt() == null ? OffsetDateTime.now() : item.capturedAt());
            m.setLatitude(item.latitude());
            m.setLongitude(item.longitude());
            m.setSortOrder(nextBase + idx);
            collectionMediaRepository.save(m);
            idx++;
        }
        if ((c.getCoverImageUrl() == null || c.getCoverImageUrl().isBlank()) && !request.items().isEmpty()) {
            c.setCoverImageUrl(request.items().getFirst().thumbnailUrl() != null
                    ? request.items().getFirst().thumbnailUrl()
                    : request.items().getFirst().mediaUrl());
            collectionRepository.save(c);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CollectionMediaDto> listMedia(String userId, UUID collectionId, Pageable pageable) {
        Collection c = ownedCollection(userId, collectionId);
        return collectionMediaRepository.findByCollectionOrderByCapturedAtDescCreatedAtDesc(c, pageable)
                .map(this::toMediaDto);
    }

    @Override
    @Transactional
    public CollectionSummaryDto ensureAutoForTrip(String userId, String tripId, String tripTitle, String coverImageUrl) {
        if (tripId == null || tripId.isBlank()) {
            throw new IllegalArgumentException("tripId is required");
        }
        Collection existing = collectionRepository
                .findByUserIdAndTypeAndTripId(userId, CollectionType.AUTO, tripId)
                .orElse(null);
        if (existing != null) {
            return toSummary(existing, countMediaFor(List.of(existing)).getOrDefault(existing.getId(), 0L));
        }
        Collection c = new Collection();
        c.setUserId(userId);
        c.setType(CollectionType.AUTO);
        c.setTripId(tripId);
        c.setTitle((tripTitle == null || tripTitle.isBlank()) ? "Trip Memories" : tripTitle.trim());
        c.setCoverImageUrl(trimToNull(coverImageUrl));
        Collection saved = collectionRepository.save(c);
        return toSummary(saved, 0);
    }

    private Collection ownedCollection(String userId, UUID collectionId) {
        return collectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Collection not found"));
    }

    private Map<UUID, Long> countMediaFor(List<Collection> collections) {
        if (collections.isEmpty()) return Map.of();
        List<UUID> ids = collections.stream().map(Collection::getId).toList();
        return collectionMediaRepository.countByCollectionIds(ids).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).longValue()
                ));
    }

    private CollectionSummaryDto toSummary(Collection c, long mediaCount) {
        return new CollectionSummaryDto(
                c.getId(),
                c.getTitle(),
                c.getType(),
                c.getTripId(),
                c.getCoverImageUrl(),
                mediaCount,
                c.getCreatedAt()
        );
    }

    private CollectionMediaDto toMediaDto(CollectionMedia m) {
        return new CollectionMediaDto(
                m.getId(),
                m.getMediaUrl(),
                m.getThumbnailUrl(),
                m.getMediaType(),
                m.getSourceType(),
                m.getSourceId(),
                m.getCapturedAt(),
                m.getLatitude(),
                m.getLongitude(),
                m.getCreatedAt()
        );
    }

    private String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
