package com.travelo.collectionservice.service;

import com.travelo.collectionservice.dto.*;
import com.travelo.collectionservice.entity.CollectionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CollectionService {
    CollectionSummaryDto create(String userId, CreateCollectionRequest request);
    Page<CollectionSummaryDto> list(String userId, CollectionType type, Pageable pageable);
    CollectionDetailDto getDetail(String userId, UUID collectionId);
    void addMedia(String userId, UUID collectionId, AddCollectionMediaRequest request);
    Page<CollectionMediaDto> listMedia(String userId, UUID collectionId, Pageable pageable);
    CollectionSummaryDto ensureAutoForTrip(String userId, String tripId, String tripTitle, String coverImageUrl);
}
